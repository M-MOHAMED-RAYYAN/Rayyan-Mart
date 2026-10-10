package com.shopping.system.dto;

import com.shopping.system.model.CartItem;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        String description,
        String imageUrl,
        BigDecimal price,
        Integer quantity,
        Integer availableStock,
        BigDecimal subtotal
) {

    public static CartItemResponse from(CartItem item) {
        BigDecimal price = item.getProduct().getPrice();
        return new CartItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getDescription(),
                item.getProduct().getImageUrl(),
                price,
                item.getQuantity(),
                item.getProduct().getQuantity(),
                price.multiply(BigDecimal.valueOf(item.getQuantity()))
        );
    }
}
