package com.shopping.system.dto;

public record LoginResponse(
        UserResponse user,
        String role,
        String message
) {
}
