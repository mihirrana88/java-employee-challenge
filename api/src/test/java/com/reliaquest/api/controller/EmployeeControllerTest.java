package com.reliaquest.api.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.model.CreateEmployeeInput;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.service.EmployeeService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

    @Test
    void getAllEmployees_Success() throws Exception {
        // Act & Assert - should return redirect to paginated endpoint
        mockMvc.perform(get("/api/v1/employee"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/api/v1/employees?page=0&size=20"));

        // Verify that service method is NOT called since this is a redirect
        verifyNoInteractions(employeeService);
    }

    @Test
    void getEmployeesByNameSearch_Success() throws Exception {
        // Act & Assert - should return redirect to paginated search endpoint
        mockMvc.perform(get("/api/v1/employee/search/John"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/api/v1/employees/search?searchString=John&page=0&size=20"));

        // Verify that service method is NOT called since this is a redirect
        verifyNoInteractions(employeeService);
    }

    @Test
    void getEmployeeById_Success() throws Exception {
        // Arrange
        Employee employee = createTestEmployee("1", "John Doe", 75000);
        when(employeeService.getEmployeeById("1")).thenReturn(employee);

        // Act & Assert
        mockMvc.perform(get("/api/v1/employee/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.employee_name").value("John Doe"));

        verify(employeeService).getEmployeeById("1");
    }

    @Test
    void getEmployeeById_NotFound() throws Exception {
        // Arrange
        when(employeeService.getEmployeeById("999")).thenThrow(new EmployeeNotFoundException("Employee not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/employee/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Employee not found"));

        verify(employeeService).getEmployeeById("999");
    }

    @Test
    void getHighestSalaryOfEmployees_Success() throws Exception {
        // Arrange
        when(employeeService.getHighestSalaryOfEmployees()).thenReturn(90000);

        // Act & Assert
        mockMvc.perform(get("/api/v1/employee/highestSalary"))
                .andExpect(status().isOk())
                .andExpect(content().string("90000"));

        verify(employeeService).getHighestSalaryOfEmployees();
    }

    @Test
    void getTopTenHighestEarningEmployeeNames_Success() throws Exception {
        // Arrange
        List<String> names = Arrays.asList("John Doe", "Jane Smith", "Bob Johnson");
        when(employeeService.getTopTenHighestEarningEmployeeNames()).thenReturn(names);

        // Act & Assert
        mockMvc.perform(get("/api/v1/employee/topTenHighestEarningEmployeeNames"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(3))
                .andExpect(jsonPath("$[0]").value("John Doe"));

        verify(employeeService).getTopTenHighestEarningEmployeeNames();
    }

    @Test
    void createEmployee_Success() throws Exception {
        // Arrange
        CreateEmployeeInput input = CreateEmployeeInput.builder()
                .name("New Employee")
                .salary(80000)
                .age(30)
                .title("Developer")
                .build();

        Employee createdEmployee = createTestEmployee("4", "New Employee", 80000);
        when(employeeService.createEmployee(any(CreateEmployeeInput.class))).thenReturn(createdEmployee);

        // Act & Assert
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("4"))
                .andExpect(jsonPath("$.employee_name").value("New Employee"));

        verify(employeeService).createEmployee(any(CreateEmployeeInput.class));
    }

    @Test
    void createEmployee_ValidationError() throws Exception {
        // Arrange
        CreateEmployeeInput invalidInput = CreateEmployeeInput.builder()
                .name("") // Invalid: blank name
                .salary(-1000) // Invalid: negative salary
                .age(15) // Invalid: age below minimum
                .title("") // Invalid: blank title
                .build();

        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidInput)))
                .andExpect(status().isBadRequest());

        verify(employeeService, never()).createEmployee(any(CreateEmployeeInput.class));
    }

    @Test
    void deleteEmployeeById_Success() throws Exception {
        when(employeeService.deleteEmployeeById("1")).thenReturn("John Doe");

        mockMvc.perform(delete("/api/v1/employee/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("John Doe"));

        verify(employeeService).deleteEmployeeById("1");
    }

    @Test
    void deleteEmployeeById_NotFound() throws Exception {
        when(employeeService.deleteEmployeeById("999")).thenThrow(new EmployeeNotFoundException("Employee not found"));

        mockMvc.perform(delete("/api/v1/employee/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Employee not found"));

        verify(employeeService).deleteEmployeeById("999");
    }

    private Employee createTestEmployee(String id, String name, Integer salary) {
        return Employee.builder()
                .id(id)
                .employeeName(name)
                .employeeSalary(salary)
                .employeeAge(30)
                .employeeTitle("Developer")
                .employeeEmail(name.toLowerCase().replace(" ", ".") + "@company.com")
                .build();
    }
}
