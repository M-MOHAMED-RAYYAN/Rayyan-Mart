package com.shopping.system.service;

import com.shopping.system.dto.ReviewRequest;
import com.shopping.system.dto.ReviewResponse;
import com.shopping.system.exception.DuplicateReviewException;
import com.shopping.system.exception.ForbiddenException;
import com.shopping.system.exception.ResourceNotFoundException;
import com.shopping.system.model.OrderStatus;
import com.shopping.system.model.Product;
import com.shopping.system.model.Review;
import com.shopping.system.model.User;
import com.shopping.system.repository.OrderRepository;
import com.shopping.system.repository.ProductRepository;
import com.shopping.system.repository.ReviewRepository;
import javax.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final SessionService sessionService;

    public ReviewService(ReviewRepository reviewRepository,
                         ProductRepository productRepository,
                         OrderRepository orderRepository,
                         SessionService sessionService) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.sessionService = sessionService;
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> listByProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found");
        }
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional
    public ReviewResponse create(HttpSession session, Long productId, ReviewRequest request) {
        User user = sessionService.getCurrentUser(session);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (reviewRepository.existsByProductIdAndUserId(productId, user.getId())) {
            throw new DuplicateReviewException("You have already reviewed this product. Edit your existing review instead.");
        }

        boolean purchased = orderRepository.existsByUserIdAndItems_Product_IdAndStatusNot(
                user.getId(), productId, OrderStatus.CANCELLED);
        if (!purchased) {
            throw new ForbiddenException("You can only review products you have purchased");
        }

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        apply(review, request);

        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional
    public ReviewResponse update(HttpSession session, Long reviewId, ReviewRequest request) {
        User user = sessionService.getCurrentUser(session);
        Review review = findOwnedReview(reviewId, user.getId());
        apply(review, request);
        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional
    public void delete(HttpSession session, Long reviewId) {
        User user = sessionService.getCurrentUser(session);
        Review review = findOwnedReview(reviewId, user.getId());
        reviewRepository.delete(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> adminList(HttpSession session) {
        sessionService.getCurrentAdmin(session);
        return reviewRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional
    public void adminDelete(HttpSession session, Long reviewId) {
        sessionService.getCurrentAdmin(session);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        reviewRepository.delete(review);
    }

    private Review findOwnedReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (!review.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to modify this review");
        }
        return review;
    }

    private void apply(Review review, ReviewRequest request) {
        review.setRating(request.rating());
        review.setComment(request.comment());
        review.setUpdatedAt(LocalDateTime.now());
    }
}