package com.shopping.system.dto;

import com.shopping.system.model.Order;
import com.shopping.system.model.OrderItem;
import com.shopping.system.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SellerOrderResponse(
        Long orderId,
        OrderStatus status,
        LocalDateTime createdAt,
        BigDecimal totalAmount,
        List<SellerOrderItemResponse> items
) {

    public static SellerOrderResponse from(Order order, Long sellerId) {
        List<SellerOrderItemResponse> sellerItems = order.getItems().stream()
                .filter(oi -> oi.getProduct() != null
                        && oi.getProduct().getSeller() != null
                        && oi.getProduct().getSeller().getId().equals(sellerId))
                .map(oi -> new SellerOrderItemResponse(
                        oi.getProduct() != null ? oi.getProduct().getId() : null,
                        oi.getProductName(),
                        oi.getQuantity(),
                        oi.getPrice(),
                        oi.getPrice().multiply(BigDecimal.valueOf(oi.getQuantity()))
                ))
                .toList();

        BigDecimal sellerTotal = sellerItems.stream()
                .map(SellerOrderItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SellerOrderResponse(order.getId(), order.getStatus(), order.getCreatedAt(), sellerTotal, sellerItems);
    }
}