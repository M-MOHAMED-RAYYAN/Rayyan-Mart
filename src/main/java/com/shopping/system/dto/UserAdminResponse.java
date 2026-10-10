package com.shopping.system.dto;

import com.shopping.system.model.User;

import java.time.LocalDateTime;

public record UserAdminResponse(
        Long id,
        String name,
        String email,
        String role,
        LocalDateTime createdAt
) {

    public static UserAdminResponse from(User user) {
        return new UserAdminResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}