package com.reliaquest.api.service;

import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.ExternalServiceException;
import com.reliaquest.api.model.ApiResponse;
import com.reliaquest.api.model.CreateEmployeeInput;
import com.reliaquest.api.model.DeleteEmployeeRequest;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.PagedResult;
import com.reliaquest.api.model.PaginationRequest;
import com.reliaquest.api.repository.EmployeeRepository;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of EmployeeService that interacts with the mock employee API.
 * Includes retry logic for resilience and comprehensive logging.
 */
@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    private final RestTemplate restTemplate;
    private final EmployeeRepository employeeRepository;
    private final String baseUrl;

    public EmployeeServiceImpl(
            RestTemplate restTemplate,
            EmployeeRepository employeeRepository,
            @Value("${app.mock-server.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.employeeRepository = employeeRepository;
        this.baseUrl = baseUrl;
    }

    @Override
    public List<Employee> getAllEmployees() {
        logger.info("Fetching all employees via repository");
        try {
            // Use repository to get all employees (this will use the snapshot pattern)
            PaginationRequest allRequest = new PaginationRequest(0, Integer.MAX_VALUE, null);
            PagedResult<Employee> result = employeeRepository.findAll(allRequest);
            logger.info(
                    "Successfully fetched {} employees via repository",
                    result.getContent().size());
            return result.getContent();
        } catch (Exception e) {
            logger.error("Error while fetching employees: {}", e.getMessage(), e);
            throw new ExternalServiceException("Failed to retrieve employees: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Employee> getEmployeesByNameSearch(String searchString) {
        logger.info("Searching employees by name: {}", searchString);
        try {
            PaginationRequest allRequest = new PaginationRequest(0, Integer.MAX_VALUE, null);
            PagedResult<Employee> result = employeeRepository.findByNameContaining(searchString, allRequest);
            logger.info("Found {} employees matching '{}'", result.getContent().size(), searchString);
            return result.getContent();
        } catch (Exception e) {
            logger.error("Error while searching employees: {}", e.getMessage(), e);
            throw new ExternalServiceException("Failed to search employees: " + e.getMessage(), e);
        }
    }

    @Override
    public PagedResult<Employee> getAllEmployeesPaged(PaginationRequest request) {
        logger.info(
                "Fetching employees with pagination: page={}, size={}, sort={}",
                request.getPage(),
                request.getSize(),
                request.getSort());
        try {
            PagedResult<Employee> result = employeeRepository.findAll(request);
            logger.info(
                    "Successfully fetched page {} with {} employees",
                    result.getPage(),
                    result.getContent().size());
            return result;
        } catch (Exception e) {
            logger.error("Error while fetching paginated employees: {}", e.getMessage(), e);
            throw new ExternalServiceException("Failed to retrieve paginated employees: " + e.getMessage(), e);
        }
    }

    @Override
    public PagedResult<Employee> getEmployeesByNameSearchPaged(String searchString, PaginationRequest request) {
        logger.info(
                "Searching employees by name with pagination: searchString={}, page={}, size={}",
                searchString,
                request.getPage(),
                request.getSize());
        try {
            PagedResult<Employee> result = employeeRepository.findByNameContaining(searchString, request);
            logger.info(
                    "Found {} employees matching '{}' on page {}",
                    result.getContent().size(),
                    searchString,
                    result.getPage());
            return result;
        } catch (Exception e) {
            logger.error("Error while searching paginated employees: {}", e.getMessage(), e);
            throw new ExternalServiceException("Failed to search paginated employees: " + e.getMessage(), e);
        }
    }

    @Override
    public Employee getEmployeeById(String id) {
        logger.info("Fetching employee by ID: {}", id);
        try {
            Employee employee = employeeRepository.findById(id);
            if (employee != null) {
                logger.info("Successfully fetched employee: {}", employee.getEmployeeName());
                return employee;
            } else {
                logger.warn("Employee not found with ID: {}", id);
                throw new EmployeeNotFoundException("Employee not found with ID: " + id);
            }
        } catch (EmployeeNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error while fetching employee by ID {}: {}", id, e.getMessage(), e);
            throw new ExternalServiceException("Failed to retrieve employee: " + e.getMessage(), e);
        }
    }

    @Override
    public Integer getHighestSalaryOfEmployees() {
        logger.info("Fetching highest salary");
        try {
            List<Employee> employees = getAllEmployees();
            if (employees.isEmpty()) {
                logger.warn("No employees found");
                return 0;
            }

            Integer highestSalary = employees.stream()
                    .filter(emp -> emp.getEmployeeSalary() != null)
                    .mapToInt(Employee::getEmployeeSalary)
                    .max()
                    .orElse(0);

            logger.info("Highest salary found: {}", highestSalary);
            return highestSalary;
        } catch (Exception e) {
            logger.error("Error while fetching highest salary: {}", e.getMessage(), e);
            throw new ExternalServiceException("Failed to retrieve highest salary: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> getTopTenHighestEarningEmployeeNames() {
        logger.info("Fetching top 10 highest earning employee names");
        try {
            List<Employee> employees = getAllEmployees();
            List<String> topEarners = employees.stream()
                    .filter(emp -> emp.getEmployeeName() != null && emp.getEmployeeSalary() != null)
                    .sorted(Comparator.comparingInt(Employee::getEmployeeSalary).reversed())
                    .limit(10)
                    .map(Employee::getEmployeeName)
                    .collect(Collectors.toList());

            logger.info("Found {} top earning employees", topEarners.size());
            return topEarners;
        } catch (Exception e) {
            logger.error("Error while fetching top earners: {}", e.getMessage(), e);
            throw new ExternalServiceException("Failed to retrieve top earners: " + e.getMessage(), e);
        }
    }

    @Override
    @Retryable(
            value = {ResourceAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000))
    public Employee createEmployee(CreateEmployeeInput employeeInput) {
        logger.info("Creating employee: {}", employeeInput.getName());
        try {
            String url = baseUrl + "/employee";
            logger.info("Making POST request to: {}", url);

            HttpEntity<CreateEmployeeInput> requestEntity = new HttpEntity<>(employeeInput);

            ResponseEntity<ApiResponse<Employee>> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<ApiResponse<Employee>>() {});

            logger.info("Received response with status: {}", response.getStatusCode());

            if (response.getBody() != null && response.getBody().getData() != null) {
                Employee createdEmployee = response.getBody().getData();
                logger.info("Successfully created employee with ID: {}", createdEmployee.getId());
                return createdEmployee;
            }

            logger.error("Failed to create employee - received null response");
            throw new ExternalServiceException("Failed to create employee - received null response");

        } catch (HttpClientErrorException e) {
            logger.error("HTTP error while creating employee: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExternalServiceException("Failed to create employee: " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            logger.error("Network error while creating employee: {}", e.getMessage());
            throw new ExternalServiceException("Network error while creating employee: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while creating employee: {}", e.getMessage(), e);
            throw new ExternalServiceException("Failed to create employee: " + e.getMessage(), e);
        }
    }

    @Override
    @Retryable(
            value = {ResourceAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000))
    public String deleteEmployeeById(String id) {
        logger.info("Deleting employee with ID: {}", id);

        try {
            // First, get the employee to check if it exists and get the name
            Employee employee = getEmployeeById(id);
            String employeeName = employee.getEmployeeName();

            String url = baseUrl + "/employee";
            logger.info("Making DELETE request to: {}", url);

            // Create request with employee name (what mock server expects)
            DeleteEmployeeRequest deleteRequest = new DeleteEmployeeRequest(employeeName);
            HttpEntity<DeleteEmployeeRequest> requestEntity = new HttpEntity<>(deleteRequest);

            ResponseEntity<ApiResponse<String>> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, requestEntity, new ParameterizedTypeReference<ApiResponse<String>>() {});

            logger.info("Received response with status: {}", response.getStatusCode());

            if (response.getBody() != null) {
                logger.info("Successfully deleted employee: {}", employeeName);
                return employeeName;
            }

            logger.error("Failed to delete employee - received null response");
            throw new ExternalServiceException("Failed to delete employee - received null response");

        } catch (EmployeeNotFoundException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error while deleting employee: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 404) {
                throw new EmployeeNotFoundException("Employee not found with ID: " + id);
            }
            throw new ExternalServiceException("Failed to delete employee: " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            logger.error("Network error while deleting employee: {}", e.getMessage());
            throw new ExternalServiceException("Network error while deleting employee: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while deleting employee: {}", e.getMessage(), e);
            throw new ExternalServiceException("Failed to delete employee: " + e.getMessage(), e);
        }
    }
}
