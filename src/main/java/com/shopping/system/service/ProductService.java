package com.shopping.system.service;

import com.shopping.system.dto.ProductRequest;
import com.shopping.system.dto.ProductResponse;
import com.shopping.system.dto.RatingCount;
import com.shopping.system.dto.RatingSummary;
import com.shopping.system.exception.ResourceNotFoundException;
import com.shopping.system.model.Product;
import com.shopping.system.repository.CartItemRepository;
import com.shopping.system.repository.OrderItemRepository;
import com.shopping.system.repository.ProductRepository;
import com.shopping.system.repository.ReviewRepository;
import javax.servlet.http.HttpSession;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewRepository reviewRepository;
    private final SessionService sessionService;

    public ProductService(ProductRepository productRepository,
                          CartItemRepository cartItemRepository,
                          OrderItemRepository orderItemRepository,
                          ReviewRepository reviewRepository,
                          SessionService sessionService) {
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderItemRepository = orderItemRepository;
        this.reviewRepository = reviewRepository;
        this.sessionService = sessionService;
    }

    public List<ProductResponse> getAll() {
        return search(null, null, null, null, null, null);
    }

    public List<ProductResponse> search(String search,
                                        String category,
                                        BigDecimal minPrice,
                                        BigDecimal maxPrice,
                                        Boolean available,
                                        String sort) {
        Specification<Product> spec = ProductRepository.withFilters(search, category, minPrice, maxPrice, available);
        Sort sortBy = resolveSort(sort);
        List<Product> products = productRepository.findAll(spec, sortBy);
        return products.stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse getById(Long id) {
        return toResponse(findProduct(id));
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.from(product, ratingSummary(product.getId()));
    }

    private RatingSummary ratingSummary(Long productId) {
        Long count = reviewRepository.countByProductId(productId);
        if (count == 0) {
            return RatingSummary.empty();
        }
        Double average = reviewRepository.averageRating(productId);
        List<Object[]> rows = reviewRepository.ratingDistribution(productId);
        List<RatingCount> distribution = new ArrayList<>();
        for (int star = 1; star <= 5; star++) {
            long starCount = 0;
            for (Object[] row : rows) {
                Integer ratingStar = ((Number) row[0]).intValue();
                if (ratingStar == star) {
                    starCount = ((Number) row[1]).longValue();
                }
            }
            distribution.add(new RatingCount(star, starCount));
        }
        return new RatingSummary(
                Double.valueOf(String.format("%.1f", average == null ? 0.0 : average)),
                count,
                distribution
        );
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by("name");
        }
        return switch (sort.trim().toLowerCase()) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "name" -> Sort.by("name");
            default -> Sort.by("name");
        };
    }

    @Transactional
    public ProductResponse create(ProductRequest request, HttpSession session) {
        sessionService.getCurrentAdmin(session);
        Product product = new Product();
        apply(product, request);
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request, HttpSession session) {
        sessionService.getCurrentAdmin(session);
        Product product = findProduct(id);
        apply(product, request);
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id, HttpSession session) {
        sessionService.getCurrentAdmin(session);
        Product product = findProduct(id);
        reviewRepository.deleteByProductId(id);
        orderItemRepository.detachProduct(id);
        cartItemRepository.deleteByProductId(id);
        productRepository.delete(product);
    }

    public List<String> categories() {
        return productRepository.findAll(Sort.by("category")).stream()
                .map(Product::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private void apply(Product product, ProductRequest request) {
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
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