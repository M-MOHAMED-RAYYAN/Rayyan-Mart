package com.shopping.system.dto;

import com.shopping.system.model.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal price,
        BigDecimal subtotal
) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProduct() != null ? item.getProduct().getId() : null,
                item.getProductName(),
                item.getQuantity(),
                item.getPrice(),
                item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
        );
    }
}
