package com.shopping.system.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name cannot exceed 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        @Size(max = 100, message = "Password cannot exceed 100 characters")
        String password,

        String role
) {
}