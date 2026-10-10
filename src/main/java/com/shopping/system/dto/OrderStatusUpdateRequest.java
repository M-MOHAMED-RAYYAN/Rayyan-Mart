package com.shopping.system.dto;

import com.shopping.system.model.OrderStatus;
import javax.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull(message = "Status is required")
        OrderStatus status
) {
}