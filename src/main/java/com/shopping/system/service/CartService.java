package com.shopping.system.service;

import com.shopping.system.dto.CartItemResponse;
import com.shopping.system.dto.CartRequest;
import com.shopping.system.dto.CartResponse;
import com.shopping.system.exception.ForbiddenException;
import com.shopping.system.exception.InsufficientStockException;
import com.shopping.system.exception.ResourceNotFoundException;
import com.shopping.system.model.Cart;
import com.shopping.system.model.CartItem;
import com.shopping.system.model.Product;
import com.shopping.system.model.User;
import com.shopping.system.repository.CartItemRepository;
import com.shopping.system.repository.CartRepository;
import com.shopping.system.repository.ProductRepository;
import javax.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final SessionService sessionService;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository,
                       SessionService sessionService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.sessionService = sessionService;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(HttpSession session) {
        User user = sessionService.getCurrentUser(session);
        Cart cart = getOrCreateCart(user);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse add(HttpSession session, CartRequest request) {
        User user = sessionService.getCurrentUser(session);
        Cart cart = getOrCreateCart(user);

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!Boolean.TRUE.equals(product.getAvailable())) {
            throw new ResourceNotFoundException("Product is not available");
        }

        Optional<CartItem> existing = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId());
        int newQuantity = request.quantity() + existing.map(CartItem::getQuantity).orElse(0);

        if (newQuantity > product.getQuantity()) {
            throw new InsufficientStockException(
                    "Only " + product.getQuantity() + " unit(s) of \"" + product.getName() + "\" available in stock");
        }

        CartItem item = existing.orElseGet(() -> {
            CartItem newItem = new CartItem();
            newItem.setProduct(product);
            cart.addItem(newItem);
            return newItem;
        });
        item.setQuantity(newQuantity);
        cartItemRepository.save(item);

        return toResponse(cart);
    }

    @Transactional
    public CartResponse updateQuantity(HttpSession session, Long itemId, Integer quantity) {
        User user = sessionService.getCurrentUser(session);
        Cart cart = getOrCreateCart(user);

        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        if (quantity > item.getProduct().getQuantity()) {
            throw new InsufficientStockException(
                    "Only " + item.getProduct().getQuantity() + " unit(s) of \""
                            + item.getProduct().getName() + "\" available in stock");
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse remove(HttpSession session, Long itemId) {
        User user = sessionService.getCurrentUser(session);
        Cart cart = getOrCreateCart(user);

        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        cart.removeItem(item);
        cartItemRepository.delete(item);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse clear(HttpSession session) {
        User user = sessionService.getCurrentUser(session);
        Cart cart = getOrCreateCart(user);
        List<CartItem> items = List.copyOf(cart.getItems());
        for (CartItem item : items) {
            cart.removeItem(item);
            cartItemRepository.delete(item);
        }
        return toResponse(cart);
    }

    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUser(user);
            return cartRepository.save(cart);
        });
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(CartItemResponse::from)
                .toList();
        BigDecimal total = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getId(), items, total);
    }
}
