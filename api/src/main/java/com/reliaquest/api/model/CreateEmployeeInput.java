package com.reliaquest.api.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input model for creating new employees.
 * Contains validation annotations to ensure data integrity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEmployeeInput {

    @NotBlank(message = "Name is required and cannot be blank")
    private String name;

    @Positive(message = "Salary must be greater than zero") @NotNull(message = "Salary is required") private Integer salary;

    @Min(value = 16, message = "Age must be at least 16")
    @Max(value = 75, message = "Age must be at most 75")
    @NotNull(message = "Age is required") private Integer age;

    @NotBlank(message = "Title is required and cannot be blank")
    private String title;
}
