package com.shopping.system.controller;

import com.shopping.system.dto.DashboardStats;
import com.shopping.system.dto.OrderResponse;
import com.shopping.system.dto.OrderStatusUpdateRequest;
import com.shopping.system.dto.ReviewResponse;
import com.shopping.system.dto.UserAdminResponse;
import com.shopping.system.service.AdminService;
import com.shopping.system.service.OrderService;
import com.shopping.system.service.ReviewService;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final OrderService orderService;
    private final AdminService adminService;
    private final ReviewService reviewService;

    public AdminController(OrderService orderService,
                           AdminService adminService,
                           ReviewService reviewService) {
        this.orderService = orderService;
        this.adminService = adminService;
        this.reviewService = reviewService;
    }

    @GetMapping("/orders")
    public List<OrderResponse> allOrders(HttpSession session) {
        return orderService.getAllOrders(session);
    }

    @GetMapping("/orders/{id}")
    public OrderResponse order(@PathVariable Long id, HttpSession session) {
        return orderService.getOrderForAdmin(session, id);
    }

    @PatchMapping("/orders/{id}/status")
    public OrderResponse updateOrderStatus(@PathVariable Long id,
                                           @Valid @RequestBody OrderStatusUpdateRequest request,
                                           HttpSession session) {
        return orderService.updateStatus(session, id, request.status());
    }

    @GetMapping("/dashboard")
    public DashboardStats dashboard(HttpSession session) {
        return adminService.dashboard(session);
    }

    @GetMapping("/users")
    public List<UserAdminResponse> users(HttpSession session) {
        return adminService.users(session);
    }

    @GetMapping("/reviews")
    public List<ReviewResponse> reviews(HttpSession session) {
        return reviewService.adminList(session);
    }

    @DeleteMapping("/reviews/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(@PathVariable Long id, HttpSession session) {
        reviewService.adminDelete(session, id);
    }
}