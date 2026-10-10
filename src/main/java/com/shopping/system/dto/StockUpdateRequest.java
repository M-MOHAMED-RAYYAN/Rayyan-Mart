package com.shopping.system.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public record StockUpdateRequest(
        @NotNull(message = "Quantity is required")
        @Min(value = 0, message = "Quantity cannot be negative")
        Integer quantity
) {
}
