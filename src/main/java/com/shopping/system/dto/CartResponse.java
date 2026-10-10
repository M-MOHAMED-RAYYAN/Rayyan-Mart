package com.shopping.system.dto;

import java.util.List;

public record CartResponse(
        Long cartId,
        List<CartItemResponse> items,
        java.math.BigDecimal total
) {
}
