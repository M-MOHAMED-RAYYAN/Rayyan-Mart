package com.shopping.system.service;

import com.shopping.system.dto.OrderResponse;
import com.shopping.system.exception.EmptyCartException;
import com.shopping.system.exception.InsufficientStockException;
import com.shopping.system.exception.InvalidOrderStatusException;
import com.shopping.system.exception.ResourceNotFoundException;
import com.shopping.system.model.Cart;
import com.shopping.system.model.CartItem;
import com.shopping.system.model.Order;
import com.shopping.system.model.OrderItem;
import com.shopping.system.model.OrderStatus;
import com.shopping.system.model.PaymentMethod;
import com.shopping.system.model.Product;
import com.shopping.system.model.User;
import com.shopping.system.repository.CartItemRepository;
import com.shopping.system.repository.OrderRepository;
import com.shopping.system.repository.ProductRepository;
import javax.servlet.http.HttpSession;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrderService {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = buildTransitions();

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final CartService cartService;
    private final SessionService sessionService;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        CartItemRepository cartItemRepository,
                        CartService cartService,
                        SessionService sessionService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.cartService = cartService;
        this.sessionService = sessionService;
    }

    private static Map<OrderStatus, Set<OrderStatus>> buildTransitions() {
        Map<OrderStatus, Set<OrderStatus>> map = new EnumMap<>(OrderStatus.class);
        map.put(OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        map.put(OrderStatus.CONFIRMED, Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED));
        map.put(OrderStatus.PROCESSING, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        map.put(OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED));
        map.put(OrderStatus.DELIVERED, Set.of());
        map.put(OrderStatus.CANCELLED, Set.of());
        return map;
    }

    @Transactional
    public OrderResponse placeOrder(HttpSession session) {
        return placeOrder(session, PaymentMethod.CASH_ON_DELIVERY);
    }

    @Transactional
    public OrderResponse placeOrder(HttpSession session, PaymentMethod paymentMethod) {
        User user = sessionService.getCurrentUser(session);
        Cart cart = cartService.getOrCreateCart(user);

        if (cart.getItems().isEmpty()) {
            throw new EmptyCartException("Your cart is empty");
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(paymentMethod == null ? PaymentMethod.CASH_ON_DELIVERY : paymentMethod);

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found: " + cartItem.getProduct().getName()));

            if (!Boolean.TRUE.equals(product.getAvailable())) {
                throw new ResourceNotFoundException("Product is no longer available: " + product.getName());
            }
            if (product.getQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for product \"" + product.getName()
                                + "\". Available: " + product.getQuantity());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());
            order.addItem(orderItem);

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));

            int newStock = product.getQuantity() - cartItem.getQuantity();
            product.setQuantity(newStock);
            if (newStock <= 0) {
                product.setAvailable(false);
            }
        }

        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);

        List<CartItem> cartItems = List.copyOf(cart.getItems());
        for (CartItem item : cartItems) {
            cart.removeItem(item);
            cartItemRepository.delete(item);
        }

        return OrderResponse.from(saved);
    }

    public List<OrderResponse> getMyOrders(HttpSession session) {
        User user = sessionService.getCurrentUser(session);
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(OrderResponse::from)
                .toList();
    }

    public OrderResponse getMyOrder(HttpSession session, Long orderId) {
        User user = sessionService.getCurrentUser(session);
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return OrderResponse.from(order);
    }

    public List<OrderResponse> getAllOrders(HttpSession session) {
        sessionService.getCurrentAdmin(session);
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(OrderResponse::from)
                .toList();
    }

    public OrderResponse getOrderForAdmin(HttpSession session, Long orderId) {
        sessionService.getCurrentAdmin(session);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse updateStatus(HttpSession session, Long orderId, OrderStatus newStatus) {
        sessionService.getCurrentAdmin(session);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() == newStatus) {
            return OrderResponse.from(order);
        }

        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.get(order.getStatus());
        if (allowed == null || !allowed.contains(newStatus)) {
            throw new InvalidOrderStatusException(
                    "Cannot change order status from " + order.getStatus() + " to " + newStatus);
        }

        order.setStatus(newStatus);
        return OrderResponse.from(orderRepository.save(order));
    }
}