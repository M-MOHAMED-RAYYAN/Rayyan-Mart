package com.shopping.system.repository;

import com.shopping.system.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByProductIdAndUserId(Long productId, Long userId);

    boolean existsByProductIdAndUserId(Long productId, Long userId);

    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

    List<Review> findAllByOrderByCreatedAtDesc();

    long countByProductId(Long productId);

    @Query("select avg(r.rating) from Review r where r.product.id = :productId")
    Double averageRating(@Param("productId") Long productId);

    @Query("select r.rating, count(r) from Review r where r.product.id = :productId group by r.rating")
    List<Object[]> ratingDistribution(@Param("productId") Long productId);

    @Modifying
    @Query("delete from Review r where r.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);
}