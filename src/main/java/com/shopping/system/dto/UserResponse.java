package com.shopping.system.dto;

import com.shopping.system.model.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        String role,
        LocalDateTime createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
