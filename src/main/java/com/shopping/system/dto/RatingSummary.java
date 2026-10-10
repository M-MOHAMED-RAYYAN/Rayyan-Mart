package com.shopping.system.dto;

import java.util.List;

public record RatingSummary(
        Double averageRating,
        Long ratingCount,
        List<RatingCount> distribution
) {

    public static RatingSummary empty() {
        return new RatingSummary(0.0, 0L, List.of());
    }
}