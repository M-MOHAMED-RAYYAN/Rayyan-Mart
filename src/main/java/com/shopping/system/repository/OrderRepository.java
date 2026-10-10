package com.shopping.system.repository;

import com.shopping.system.model.Order;
import com.shopping.system.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    List<Order> findTop10ByOrderByCreatedAtDesc();

    @Query("select distinct o from Order o join o.items oi where oi.product is not null and oi.product.seller.id = :sellerId order by o.createdAt desc")
    List<Order> findBySellerProducts(@Param("sellerId") Long sellerId);

    boolean existsByUserIdAndItems_Product_IdAndStatusNot(Long userId, Long productId, OrderStatus status);

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o")
    BigDecimal sumTotalAmount();
}
