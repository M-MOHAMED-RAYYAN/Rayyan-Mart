package com.shopping.system.service;

import com.shopping.system.dto.DashboardStats;
import com.shopping.system.dto.OrderResponse;
import com.shopping.system.dto.UserAdminResponse;
import com.shopping.system.model.Role;
import com.shopping.system.model.User;
import com.shopping.system.repository.OrderRepository;
import com.shopping.system.repository.ProductRepository;
import com.shopping.system.repository.UserRepository;
import javax.servlet.http.HttpSession;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AdminService {

    private final SessionService sessionService;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public AdminService(SessionService sessionService,
                        UserRepository userRepository,
                        ProductRepository productRepository,
                        OrderRepository orderRepository) {
        this.sessionService = sessionService;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    public DashboardStats dashboard(HttpSession session) {
        sessionService.getCurrentAdmin(session);

        DashboardStats stats = new DashboardStats();
        stats.setTotalUsers(userRepository.count());
        stats.setTotalCustomers(userRepository.countByRole(Role.CUSTOMER));
        stats.setTotalSellers(userRepository.countByRole(Role.SELLER));
        stats.setTotalProducts(productRepository.count());
        stats.setTotalOrders(orderRepository.count());
        BigDecimal revenue = orderRepository.sumTotalAmount();
        stats.setTotalRevenue(revenue == null ? BigDecimal.ZERO : revenue);
        stats.setRecentOrders(orderRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(OrderResponse::from)
                .toList());
        return stats;
    }

    public List<UserAdminResponse> users(HttpSession session) {
        sessionService.getCurrentAdmin(session);
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(UserAdminResponse::from)
                .toList();
    }

    public User getAdmin(HttpSession session) {
        return sessionService.getCurrentAdmin(session);
    }
}