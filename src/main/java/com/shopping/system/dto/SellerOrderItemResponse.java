package com.shopping.system.dto;

import java.math.BigDecimal;

public record SellerOrderItemResponse(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal price,
        BigDecimal subtotal
) {
    public BigDecimal subtotal() {
        return subtotal;
    }
}