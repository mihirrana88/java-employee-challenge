package com.reliaquest.api.controller;

import com.reliaquest.api.model.CreateEmployeeInput;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.service.EmployeeService;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for employee operations.
 * Implements the IEmployeeController interface to fulfill the API contract.
 *
 * This controller serves as the entry point for all employee-related HTTP requests
 * and delegates business logic to the EmployeeService.
 */
@RestController
@RequestMapping("/api/v1/employee")
public class EmployeeController implements IEmployeeController<Employee, CreateEmployeeInput> {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);

    private final EmployeeService employeeService;
    private final int defaultPageSize;
    private final int maxPageSize;

    public EmployeeController(
            EmployeeService employeeService,
            @Value("${app.pagination.default-page-size:20}") int defaultPageSize,
            @Value("${app.pagination.max-page-size:100}") int maxPageSize) {
        this.employeeService = employeeService;
        this.defaultPageSize = defaultPageSize;
        this.maxPageSize = maxPageSize;
    }

    /**
     * Retrieves all employees (interface implementation).
     * Redirects to paginated endpoint for better scalability.
     */
    @Override
    public ResponseEntity<List<Employee>> getAllEmployees() {
        logger.info("Redirecting getAllEmployees request to paginated endpoint");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "/api/v1/employees?page=0&size=20");
        return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
    }

    /**
     * Searches for employees by name containing the search string.
     * Redirects to paginated search endpoint for better Scalability.
     */
    @Override
    public ResponseEntity<List<Employee>> getEmployeesByNameSearch(@PathVariable String searchString) {
        logger.info("Redirecting search request for '{}' to paginated endpoint", searchString);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "/api/v1/employees/search?searchString=" + searchString + "&page=0&size=20");
        return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
    }

    /**
     * Retrieves a single employee by ID.
     */
    @Override
    public ResponseEntity<Employee> getEmployeeById(@PathVariable String id) {
        logger.info("Received request to get employee by ID: {}", id);
        Employee employee = employeeService.getEmployeeById(id);
        logger.info("Successfully retrieved employee with ID: {}", id);
        return ResponseEntity.ok(employee);
    }

    /**
     * Gets the highest salary among all employees.
     */
    @Override
    public ResponseEntity<Integer> getHighestSalaryOfEmployees() {
        logger.info("Received request to get highest salary");
        Integer highestSalary = employeeService.getHighestSalaryOfEmployees();
        logger.info("Highest salary found: {}", highestSalary);
        return ResponseEntity.ok(highestSalary);
    }

    /**
     * Gets the names of the top 10 highest earning employees.
     */
    @Override
    public ResponseEntity<List<String>> getTopTenHighestEarningEmployeeNames() {
        logger.info("Received request to get top 10 highest earning employee names");
        List<String> topEarners = employeeService.getTopTenHighestEarningEmployeeNames();
        logger.info("Found {} top earning employees", topEarners.size());
        return ResponseEntity.ok(topEarners);
    }

    /**
     * Creates a new employee.
     */
    @Override
    public ResponseEntity<Employee> createEmployee(@Valid @RequestBody CreateEmployeeInput employeeInput) {
        logger.info("Received request to create employee: {}", employeeInput.getName());
        Employee createdEmployee = employeeService.createEmployee(employeeInput);
        logger.info("Successfully created employee with ID: {}", createdEmployee.getId());
        return ResponseEntity.ok(createdEmployee);
    }

    /**
     * Deletes an employee by ID.
     */
    @Override
    public ResponseEntity<String> deleteEmployeeById(@PathVariable String id) {
        logger.info("Received request to delete employee by ID: {}", id);
        String result = employeeService.deleteEmployeeById(id);
        logger.info("Successfully deleted employee with ID: {}", id);
        return ResponseEntity.ok(result);
    }
}
