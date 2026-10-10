package com.shopping.system.dto;

import com.shopping.system.model.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer quantity,
        String imageUrl,
        String category,
        Boolean available,
        Long sellerId,
        String sellerName,
        LocalDateTime createdAt,
        Double averageRating,
        Long ratingCount,
        List<RatingCount> ratingDistribution
) {

    public static ProductResponse from(Product product) {
        return from(product, null);
    }

    public static ProductResponse from(Product product, RatingSummary rating) {
        Long sellerId = null;
        String sellerName = null;
        try {
            if (product.getSeller() != null) {
                sellerId = product.getSeller().getId();
                sellerName = product.getSeller().getName();
            }
        } catch (Exception ignored) {
            // lazy proxy not initialized (e.g. detached) - leave seller fields null
        }
        Double average = rating != null ? rating.averageRating() : null;
        Long count = rating != null ? rating.ratingCount() : null;
        List<RatingCount> distribution = rating != null ? rating.distribution() : null;
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantity(),
                product.getImageUrl(),
                product.getCategory(),
                product.getAvailable(),
                sellerId,
                sellerName,
                product.getCreatedAt(),
                average,
                count,
                distribution
        );
    }
}