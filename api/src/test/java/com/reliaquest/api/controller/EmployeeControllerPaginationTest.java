package com.reliaquest.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.PagedResult;
import com.reliaquest.api.model.PaginationRequest;
import com.reliaquest.api.service.EmployeeService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for pagination functionality in PaginatedEmployeeController.
 * Tests the paginated endpoints and their parameter handling.
 */
@WebMvcTest(PaginatedEmployeeController.class)
class EmployeeControllerPaginationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @Autowired
    private ObjectMapper objectMapper;

    private Employee employee1;
    private Employee employee2;
    private Employee employee3;

    @BeforeEach
    void setUp() {
        employee1 = new Employee();
        employee1.setId("1");
        employee1.setEmployeeName("John Doe");
        employee1.setEmployeeSalary(75000);
        employee1.setEmployeeAge(30);

        employee2 = new Employee();
        employee2.setId("2");
        employee2.setEmployeeName("Jane Smith");
        employee2.setEmployeeSalary(85000);
        employee2.setEmployeeAge(25);

        employee3 = new Employee();
        employee3.setId("3");
        employee3.setEmployeeName("Bob Johnson");
        employee3.setEmployeeSalary(55000);
        employee3.setEmployeeAge(35);
    }

    @Test
    void getAllEmployeesPaged_WithDefaultParameters_ShouldReturnFirstPage() throws Exception {
        List<Employee> employees = Arrays.asList(employee1, employee2);
        PagedResult<Employee> pagedResult = new PagedResult<>(employees, 0, 20, 25L);

        when(employeeService.getAllEmployeesPaged(any(PaginationRequest.class))).thenReturn(pagedResult);

        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value("1"))
                .andExpect(jsonPath("$.content[0].employee_name").value("John Doe"))
                .andExpect(jsonPath("$.content[1].id").value("2"))
                .andExpect(jsonPath("$.content[1].employee_name").value("Jane Smith"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.total_elements").value(25))
                .andExpect(jsonPath("$.total_pages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        verify(employeeService)
                .getAllEmployeesPaged(
                        ArgumentMatchers.argThat(request -> request.getPage() == 0 && request.getSize() == 20));
    }

    @Test
    void getAllEmployeesPaged_WithCustomParameters_ShouldReturnRequestedPage() throws Exception {
        List<Employee> employees = Arrays.asList(employee3);
        PagedResult<Employee> pagedResult = new PagedResult<>(employees, 2, 5, 25L);

        when(employeeService.getAllEmployeesPaged(any(PaginationRequest.class))).thenReturn(pagedResult);

        mockMvc.perform(get("/api/v1/employees")
                        .param("page", "2")
                        .param("size", "5")
                        .param("sort", "name"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value("3"))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5));

        verify(employeeService)
                .getAllEmployeesPaged(ArgumentMatchers.argThat(request ->
                        request.getPage() == 2 && request.getSize() == 5 && "name".equals(request.getSort())));
    }

    @Test
    void getAllEmployeesPaged_WithMaxSizeExceeded_ShouldCapAtMaxSize() throws Exception {
        List<Employee> employees = Arrays.asList(employee1, employee2);
        PagedResult<Employee> pagedResult = new PagedResult<>(employees, 0, 100, 25L);

        when(employeeService.getAllEmployeesPaged(any(PaginationRequest.class))).thenReturn(pagedResult);

        mockMvc.perform(get("/api/v1/employees").param("page", "0").param("size", "200")) // Should be capped at 100
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));

        verify(employeeService).getAllEmployeesPaged(ArgumentMatchers.argThat(request -> request.getSize() == 100));
    }

    @Test
    void getEmployeesByNameSearchPaged_WithSearchString_ShouldReturnFilteredResults() throws Exception {
        List<Employee> employees = Arrays.asList(employee1);
        PagedResult<Employee> pagedResult = new PagedResult<>(employees, 0, 20, 1L);

        when(employeeService.getEmployeesByNameSearchPaged(eq("John"), any(PaginationRequest.class)))
                .thenReturn(pagedResult);

        mockMvc.perform(get("/api/v1/employees/search").param("searchString", "John"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].employee_name").value("John Doe"))
                .andExpect(jsonPath("$.total_elements").value(1));

        verify(employeeService)
                .getEmployeesByNameSearchPaged(
                        eq("John"),
                        ArgumentMatchers.argThat(request -> request.getPage() == 0 && request.getSize() == 20));
    }

    @Test
    void getEmployeesByNameSearchPaged_WithAllParameters_ShouldPassThemCorrectly() throws Exception {
        List<Employee> employees = Arrays.asList(employee2);
        PagedResult<Employee> pagedResult = new PagedResult<>(employees, 1, 10, 1L);

        when(employeeService.getEmployeesByNameSearchPaged(eq("Smith"), any(PaginationRequest.class)))
                .thenReturn(pagedResult);

        mockMvc.perform(get("/api/v1/employees/search")
                        .param("searchString", "Smith")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "salary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].employee_name").value("Jane Smith"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10));

        verify(employeeService)
                .getEmployeesByNameSearchPaged(
                        eq("Smith"),
                        ArgumentMatchers.argThat(request -> request.getPage() == 1
                                && request.getSize() == 10
                                && "salary".equals(request.getSort())));
    }

    @Test
    void getEmployeesByNameSearchPaged_WithEmptyResults_ShouldReturnEmptyPage() throws Exception {
        List<Employee> employees = Collections.emptyList();
        PagedResult<Employee> pagedResult = new PagedResult<>(employees, 0, 20, 0L);

        when(employeeService.getEmployeesByNameSearchPaged(eq("NonExistent"), any(PaginationRequest.class)))
                .thenReturn(pagedResult);

        mockMvc.perform(get("/api/v1/employees/search").param("searchString", "NonExistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.total_elements").value(0))
                .andExpect(jsonPath("$.total_pages").value(0));
    }
}
