package com.shopping.system.dto;

import com.shopping.system.model.Review;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long productId,
        Long userId,
        String userName,
        Integer rating,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getUser().getId(),
                review.getUser().getName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}