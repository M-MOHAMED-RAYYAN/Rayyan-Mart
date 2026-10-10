package com.shopping.system.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public record CartRequest(
        @NotNull(message = "productId is required")
        Long productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be greater than zero")
        Integer quantity
) {
}
