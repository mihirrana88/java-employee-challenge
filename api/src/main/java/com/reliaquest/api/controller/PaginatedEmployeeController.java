package com.reliaquest.api.controller;

import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.PagedResult;
import com.reliaquest.api.model.PaginationRequest;
import com.reliaquest.api.service.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for paginated employee operations.
 * Provides end-user facing APIs for paginated employee data retrieval.
 *
 * This controller handles:
 * - Paginated employee listings
 * - Paginated employee search
 */
@RestController
@RequestMapping("/api/v1/employees")
public class PaginatedEmployeeController {

    private static final Logger logger = LoggerFactory.getLogger(PaginatedEmployeeController.class);

    private final EmployeeService employeeService;
    private final int defaultPageSize;
    private final int maxPageSize;

    public PaginatedEmployeeController(
            EmployeeService employeeService,
            @Value("${app.pagination.default-page-size:20}") int defaultPageSize,
            @Value("${app.pagination.max-page-size:100}") int maxPageSize) {
        this.employeeService = employeeService;
        this.defaultPageSize = defaultPageSize;
        this.maxPageSize = maxPageSize;
    }

    /**
     * Retrieves employees with pagination support.
     * End-user facing paginated API.
     */
    @GetMapping
    public ResponseEntity<PagedResult<Employee>> getAllEmployeesPaginated(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(required = false) String sort) {

        int actualSize = Math.min(size, maxPageSize);
        PaginationRequest request = new PaginationRequest(page, actualSize, sort);

        logger.info("Received paginated employees request: page={}, size={}, sort={}", page, actualSize, sort);

        PagedResult<Employee> result = employeeService.getAllEmployeesPaged(request);

        logger.info(
                "Returning page {} of {} with {} employees (total: {})",
                result.getPage(),
                result.getTotalPages(),
                result.getContent().size(),
                result.getTotalElements());

        return ResponseEntity.ok(result);
    }

    /**
     * Searches for employees by name with pagination support.
     * End-user facing paginated search API.
     */
    @GetMapping("/search")
    public ResponseEntity<PagedResult<Employee>> searchEmployeesPaginated(
            @RequestParam String searchString,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(required = false) String sort) {

        int actualSize = Math.min(size, maxPageSize);
        PaginationRequest request = new PaginationRequest(page, actualSize, sort);

        logger.info(
                "Received paginated search request: searchString={}, page={}, size={}, sort={}",
                searchString,
                page,
                actualSize,
                sort);

        PagedResult<Employee> result = employeeService.getEmployeesByNameSearchPaged(searchString, request);

        logger.info(
                "Found {} employees matching '{}' on page {} of {}",
                result.getContent().size(),
                searchString,
                result.getPage(),
                result.getTotalPages());

        return ResponseEntity.ok(result);
    }
}
