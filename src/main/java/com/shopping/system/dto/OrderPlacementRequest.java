package com.shopping.system.dto;

import com.shopping.system.model.PaymentMethod;

/**
 * Optional body for POST /api/orders. When omitted (or when paymentMethod is
 * null) the order defaults to Cash on Delivery.
 */
public record OrderPlacementRequest(PaymentMethod paymentMethod) {
}