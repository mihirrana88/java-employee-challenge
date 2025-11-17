package com.reliaquest.api.repository.impl;

import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.ExternalServiceException;
import com.reliaquest.api.model.ApiResponse;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.PagedResult;
import com.reliaquest.api.model.PaginationRequest;
import com.reliaquest.api.repository.EmployeeRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Smart Snapshot Pattern implementation for Employee Repository.
 *
 * Features:
 * - TTL-based caching with automatic refresh
 * - Thread-safe snapshot management
 * - Memory-efficient pagination
 * - Single server call for multiple page requests
 * - Graceful degradation on failures
 */
public class SnapshotEmployeeRepository implements EmployeeRepository {

    private static final Logger logger = LoggerFactory.getLogger(SnapshotEmployeeRepository.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final Duration snapshotTtl;

    // Thread-safe snapshot management
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile List<Employee> snapshot;
    private volatile Instant snapshotTime;

    public SnapshotEmployeeRepository(RestTemplate restTemplate, String baseUrl, Duration snapshotTtl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.snapshotTtl = snapshotTtl;
    }

    @Override
    public List<Employee> findAll() {
        ensureFreshSnapshot();
        lock.readLock().lock();
        try {
            return snapshot != null ? List.copyOf(snapshot) : Collections.emptyList();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public PagedResult<Employee> findAll(PaginationRequest pagination) {
        ensureFreshSnapshot();
        lock.readLock().lock();
        try {
            if (snapshot == null || snapshot.isEmpty()) {
                return new PagedResult<>(Collections.emptyList(), pagination.getPage(), pagination.getSize(), 0);
            }

            return createPagedResult(snapshot, pagination);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public PagedResult<Employee> findByNameContaining(String searchString, PaginationRequest pagination) {
        ensureFreshSnapshot();
        lock.readLock().lock();
        try {
            if (snapshot == null) {
                return new PagedResult<>(Collections.emptyList(), pagination.getPage(), pagination.getSize(), 0);
            }

            List<Employee> filtered = snapshot.stream()
                    .filter(employee -> employee.getEmployeeName() != null
                            && employee.getEmployeeName().toLowerCase().contains(searchString.toLowerCase()))
                    .toList();

            return createPagedResult(filtered, pagination);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Employee findById(String id) {
        ensureFreshSnapshot();
        lock.readLock().lock();
        try {
            if (snapshot == null) {
                throw new EmployeeNotFoundException("Employee not found with ID: " + id);
            }

            return snapshot.stream()
                    .filter(employee -> id.equals(employee.getId()))
                    .findFirst()
                    .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with ID: " + id));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<Employee> streamAll() {
        ensureFreshSnapshot();
        lock.readLock().lock();
        try {
            return snapshot != null ? snapshot.stream() : Stream.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void refresh() {
        logger.info("Forcing refresh of employee snapshot");
        refreshSnapshot();
    }

    @Override
    public long count() {
        ensureFreshSnapshot();
        lock.readLock().lock();
        try {
            return snapshot != null ? snapshot.size() : 0;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Ensures that the snapshot is fresh based on TTL.
     * Uses double-checked locking pattern for performance.
     */
    private void ensureFreshSnapshot() {
        // Fast path - check if snapshot is still valid
        if (snapshot != null
                && snapshotTime != null
                && snapshotTime.plus(snapshotTtl).isAfter(Instant.now())) {
            return;
        }

        // Slow path - need to refresh
        refreshSnapshot();
    }

    /**
     * Refreshes the snapshot by fetching fresh data from the external service.
     * Thread-safe implementation with proper error handling and retry logic.
     */
    @Retryable(
            value = {ResourceAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000))
    private void refreshSnapshot() {
        lock.writeLock().lock();
        try {
            // Double-check pattern - another thread might have refreshed while waiting
            if (snapshot != null
                    && snapshotTime != null
                    && snapshotTime.plus(snapshotTtl).isAfter(Instant.now())) {
                return;
            }

            logger.info("Refreshing employee snapshot from external service");
            Instant startTime = Instant.now();

            try {
                String url = baseUrl + "/employee";
                ResponseEntity<ApiResponse<List<Employee>>> response = restTemplate.exchange(
                        url, HttpMethod.GET, null, new ParameterizedTypeReference<ApiResponse<List<Employee>>>() {});

                if (response.getBody() != null && response.getBody().getData() != null) {
                    List<Employee> freshData = response.getBody().getData();

                    // Atomic update
                    this.snapshot = List.copyOf(freshData);
                    this.snapshotTime = Instant.now();

                    long refreshDuration =
                            Duration.between(startTime, Instant.now()).toMillis();
                    logger.info(
                            "Successfully refreshed employee snapshot: {} employees in {}ms",
                            freshData.size(),
                            refreshDuration);
                } else {
                    logger.warn("Received null or empty response from external service");
                    handleRefreshFailure();
                }
            } catch (Exception e) {
                logger.error("Failed to refresh employee snapshot: {}", e.getMessage(), e);
                handleRefreshFailure();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Handles refresh failures with graceful degradation.
     */
    private void handleRefreshFailure() {
        if (snapshot == null) {
            // No cached data available, throw exception
            throw new ExternalServiceException("Failed to fetch employee data and no cached data available");
        }

        // Keep stale data and log warning
        logger.warn(
                "Using stale employee data due to refresh failure. Data age: {}",
                Duration.between(snapshotTime, Instant.now()));
    }

    /**
     * Creates a paginated result from a list of employees.
     * Memory-efficient implementation using subList.
     */
    private PagedResult<Employee> createPagedResult(List<Employee> employees, PaginationRequest pagination) {
        int totalElements = employees.size();
        int start = (int) Math.min(pagination.getOffset(), totalElements);
        int end = Math.min(start + pagination.getSize(), totalElements);

        List<Employee> pageContent = start < totalElements ? employees.subList(start, end) : Collections.emptyList();

        return new PagedResult<>(pageContent, pagination.getPage(), pagination.getSize(), totalElements);
    }
}
