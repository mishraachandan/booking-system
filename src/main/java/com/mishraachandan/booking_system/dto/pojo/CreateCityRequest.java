package com.mishraachandan.booking_system.dto.pojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a City. Restricts the fields a caller can set to avoid
 * JPA mass-assignment issues (id, createdAt, updatedAt, etc.).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCityRequest {

    @NotBlank
    @Size(min = 1, max = 100)
    private String name;
}
