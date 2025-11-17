package com.reliaquest.api.service;

import com.reliaquest.api.model.CreateEmployeeInput;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.PagedResult;
import com.reliaquest.api.model.PaginationRequest;
import java.util.List;

/**
 * Service interface for employee operations.
 * Defines the contract for employee-related business logic.
 */
public interface EmployeeService {

    /**
     * Retrieves all employees from the external service.
     *
     * @return List of all employees
     */
    List<Employee> getAllEmployees();

    /**
     * Retrieves all employees with pagination support.
     * Uses Smart Snapshot Pattern for efficient memory usage and consistent results.
     *
     * @param request Pagination parameters (page number, size, sort)
     * @return Paginated result containing employees and pagination metadata
     */
    PagedResult<Employee> getAllEmployeesPaged(PaginationRequest request);

    /**
     * Searches for employees by name containing the search string.
     *
     * @param searchString The string to search for in employee names
     * @return List of employees matching the search criteria
     */
    List<Employee> getEmployeesByNameSearch(String searchString);

    /**
     * Searches for employees by name with pagination support.
     * Uses Smart Snapshot Pattern for efficient memory usage and consistent results.
     *
     * @param searchString The string to search for in employee names
     * @param request Pagination parameters (page number, size, sort)
     * @return Paginated result containing employees and pagination metadata
     */
    PagedResult<Employee> getEmployeesByNameSearchPaged(String searchString, PaginationRequest request);

    /**
     * Retrieves a single employee by ID.
     *
     * @param id the employee ID
     * @return the employee with the specified ID
     * @throws com.reliaquest.api.exception.EmployeeNotFoundException if employee is not found
     */
    Employee getEmployeeById(String id);

    /**
     * Gets the highest salary among all employees.
     *
     * @return the highest salary value
     */
    Integer getHighestSalaryOfEmployees();

    /**
     * Gets the names of the top 10 highest earning employees.
     *
     * @return List of employee names sorted by salary in descending order (top 10)
     */
    List<String> getTopTenHighestEarningEmployeeNames();

    /**
     * Creates a new employee.
     *
     * @param employeeInput the employee data to create
     * @return the created employee
     */
    Employee createEmployee(CreateEmployeeInput employeeInput);

    /**
     * Deletes an employee by ID.
     *
     * @param id the employee ID to delete
     * @return the name of the deleted employee
     * @throws com.reliaquest.api.exception.EmployeeNotFoundException if employee is not found
     */
    String deleteEmployeeById(String id);
}
