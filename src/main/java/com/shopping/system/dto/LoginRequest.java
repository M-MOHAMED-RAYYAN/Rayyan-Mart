package com.shopping.system.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
