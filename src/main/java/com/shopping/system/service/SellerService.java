package com.shopping.system.service;

import com.shopping.system.dto.ProductRequest;
import com.shopping.system.dto.ProductResponse;
import com.shopping.system.dto.SellerDashboardResponse;
import com.shopping.system.dto.SellerOrderResponse;
import com.shopping.system.exception.ForbiddenException;
import com.shopping.system.exception.ResourceNotFoundException;
import com.shopping.system.model.Order;
import com.shopping.system.model.Product;
import com.shopping.system.model.User;
import com.shopping.system.repository.CartItemRepository;
import com.shopping.system.repository.OrderItemRepository;
import com.shopping.system.repository.OrderRepository;
import com.shopping.system.repository.ProductRepository;
import com.shopping.system.repository.ReviewRepository;
import javax.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class SellerService {

    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final SessionService sessionService;

    public SellerService(ProductRepository productRepository,
                         CartItemRepository cartItemRepository,
                         OrderItemRepository orderItemRepository,
                         OrderRepository orderRepository,
                         ReviewRepository reviewRepository,
                         SessionService sessionService) {
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
        this.reviewRepository = reviewRepository;
        this.sessionService = sessionService;
    }

    @Transactional(readOnly = true)
    public SellerDashboardResponse dashboard(HttpSession session) {
        User seller = sessionService.getCurrentSeller(session);
        List<Product> mine = productRepository.findBySellerOrderByCreatedAtDesc(seller);

        long total = mine.size();
        long available = mine.stream().filter(p -> Boolean.TRUE.equals(p.getAvailable())).count();
        long outOfStock = total - available;
        long totalStock = mine.stream()
                .mapToLong(p -> p.getQuantity() == null ? 0 : p.getQuantity())
                .sum();
        List<ProductResponse> recent = mine.stream()
                .sorted(Comparator.comparing(Product::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(5)
                .map(ProductResponse::from)
                .toList();

        return new SellerDashboardResponse(
                seller.getName(), total, available, outOfStock, totalStock, recent);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> myProducts(HttpSession session) {
        User seller = sessionService.getCurrentSeller(session);
        return productRepository.findBySellerOrderByCreatedAtDesc(seller).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SellerOrderResponse> myOrders(HttpSession session) {
        User seller = sessionService.getCurrentSeller(session);
        return orderRepository.findBySellerProducts(seller.getId()).stream()
                .map(order -> SellerOrderResponse.from(order, seller.getId()))
                .toList();
    }

    @Transactional
    public ProductResponse create(ProductRequest request, HttpSession session) {
        User seller = sessionService.getCurrentSeller(session);
        Product product = new Product();
        apply(product, request);
        product.setSeller(seller);
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request, HttpSession session) {
        User seller = sessionService.getCurrentSeller(session);
        Product product = findOwnedProduct(id, seller);
        apply(product, request);
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id, HttpSession session) {
        User seller = sessionService.getCurrentSeller(session);
        Product product = findOwnedProduct(id, seller);
        reviewRepository.deleteByProductId(id);
        orderItemRepository.detachProduct(id);
        cartItemRepository.deleteByProductId(id);
        productRepository.delete(product);
    }

    @Transactional
    public ProductResponse updateStock(Long id, Integer quantity, HttpSession session) {
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        User seller = sessionService.getCurrentSeller(session);
        Product product = findOwnedProduct(id, seller);
        product.setQuantity(quantity);
        product.setAvailable(quantity > 0);
        return ProductResponse.from(productRepository.save(product));
    }

    private Product findOwnedProduct(Long id, User seller) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (product.getSeller() == null || !product.getSeller().getId().equals(seller.getId())) {
            throw new ForbiddenException("You do not have permission to manage this product");
        }
        return product;
    }

    private void apply(Product product, ProductRequest request) {
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        if (request.quantity() < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        product.setQuantity(request.quantity());
        product.setImageUrl(normalize(request.imageUrl()));
        product.setCategory(normalize(request.category()));
        Boolean available = request.available();
        product.setAvailable(available != null ? available : request.quantity() > 0);
        if (request.quantity() == 0) {
            product.setAvailable(false);
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}