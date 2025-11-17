package com.reliaquest.api.config;

import com.reliaquest.api.repository.EmployeeRepository;
import com.reliaquest.api.repository.impl.SnapshotEmployeeRepository;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for pagination-related components.
 * Sets up the Smart Snapshot Pattern repository implementation.
 */
@Configuration
public class PaginationConfig {

    @Bean
    public EmployeeRepository employeeRepository(
            RestTemplate restTemplate,
            @Value("${app.mock-server.base-url}") String baseUrl,
            @Value("${app.pagination.cache-ttl-minutes:5}") int cacheTtlMinutes) {

        return new SnapshotEmployeeRepository(restTemplate, baseUrl, Duration.ofMinutes(cacheTtlMinutes));
    }
}
