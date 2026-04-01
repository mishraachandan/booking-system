package com.mishraachandan.booking_system.dto.pojo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserRegistrationRequest {

    @NotBlank
    @Email(message = "Invalid email address")   
    private String email;

    @NotBlank
    @Size(min = 8,  max = 16, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank
    @Size(min = 2, max = 30, message = "First name must be between 2 and 30 characters")
    private String firstName;

    @NotBlank
    @Size(min = 2, max = 30, message = "Last name must be between 2 and 30 characters")
    private String lastName;

    @NotBlank
    @Pattern(
        regexp = "^[+]?[(]?[0-9]{1,4}[)]?[-\\s./0-9]{7,14}$",
        message = "Please enter a valid phone number (e.g. +91 9988776655)"
    )
    private String phone;

}
