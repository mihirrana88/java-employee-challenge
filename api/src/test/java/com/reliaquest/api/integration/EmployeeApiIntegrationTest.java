package com.reliaquest.api.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration tests for the Employee API.
 * These tests verify the complete application context and configuration.
 */
@SpringBootTest
@TestPropertySource(
        properties = {"app.mock-server.base-url=http://localhost:8112/api/v1", "logging.level.com.reliaquest.api=DEBUG"
        })
class EmployeeApiIntegrationTest {

    @Test
    void contextLoads() {
        // Test that the Spring application context loads successfully
        // This test verifies that all beans are properly configured and the application starts
    }
}
