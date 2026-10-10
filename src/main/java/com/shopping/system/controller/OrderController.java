package com.shopping.system.controller;

import com.shopping.system.dto.OrderPlacementRequest;
import com.shopping.system.dto.OrderResponse;
import com.shopping.system.model.PaymentMethod;
import com.shopping.system.service.OrderService;
import javax.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(@RequestBody(required = false) OrderPlacementRequest request,
                                    HttpSession session) {
        PaymentMethod paymentMethod = request == null || request.paymentMethod() == null
                ? PaymentMethod.CASH_ON_DELIVERY
                : request.paymentMethod();
        return orderService.placeOrder(session, paymentMethod);
    }

    @GetMapping
    public List<OrderResponse> myOrders(HttpSession session) {
        return orderService.getMyOrders(session);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id, HttpSession session) {
        return orderService.getMyOrder(session, id);
    }
}