package com.reliaquest.api.repository;

import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.PagedResult;
import com.reliaquest.api.model.PaginationRequest;
import java.util.List;
import java.util.stream.Stream;

/**
 * Repository interface for employee data operations.
 * Provides abstraction layer for different data sources.
 */
public interface EmployeeRepository {

    /**
     * Retrieves all employees from the data source.
     * This method may cache results for performance optimization.
     *
     * @return List of all employees
     */
    List<Employee> findAll();

    /**
     * Retrieves a paginated list of employees.
     * Uses smart snapshot caching for performance.
     *
     * @param pagination pagination parameters
     * @return paginated result containing employees and metadata
     */
    PagedResult<Employee> findAll(PaginationRequest pagination);

    /**
     * Searches for employees by name with pagination support.
     *
     * @param searchString the string to search for in employee names
     * @param pagination pagination parameters
     * @return paginated result of employees matching the search criteria
     */
    PagedResult<Employee> findByNameContaining(String searchString, PaginationRequest pagination);

    /**
     * Finds an employee by their ID.
     *
     * @param id the employee ID
     * @return the employee if found
     * @throws com.reliaquest.api.exception.EmployeeNotFoundException if not found
     */
    Employee findById(String id);

    /**
     * Returns a stream of all employees for complex operations.
     * Uses the cached snapshot when available.
     *
     * @return stream of employees
     */
    Stream<Employee> streamAll();

    /**
     * Forces a refresh of the cached data.
     * Useful when fresh data is required immediately.
     */
    void refresh();

    /**
     * Gets the total count of employees.
     * Uses cached data when available for performance.
     *
     * @return total number of employees
     */
    long count();
}
