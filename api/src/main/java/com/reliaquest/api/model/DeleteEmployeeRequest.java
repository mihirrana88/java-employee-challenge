package com.reliaquest.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for deleting an employee.
 * Must match the structure expected by the mock server (DeleteMockEmployeeInput).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteEmployeeRequest {

    /**
     * The name of the employee to delete.
     * The mock server deletes employees by name, not by ID.
     */
    private String name;
}
