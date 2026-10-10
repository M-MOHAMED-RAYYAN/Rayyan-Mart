package com.shopping.system.dto;

import com.shopping.system.model.Order;
import com.shopping.system.model.OrderStatus;
import com.shopping.system.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String customerName,
        BigDecimal totalAmount,
        OrderStatus status,
        PaymentMethod paymentMethod,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {

    public static OrderResponse from(Order order) {
        List<OrderItemResponse> orderItems = order.getItems().stream()
                .map(OrderItemResponse::from)
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getUser().getName(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getCreatedAt(),
                orderItems
        );
    }
}
