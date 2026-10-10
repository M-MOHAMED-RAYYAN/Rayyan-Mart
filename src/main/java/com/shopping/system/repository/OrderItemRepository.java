package com.shopping.system.repository;

import com.shopping.system.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Modifying
    @Query("update OrderItem oi set oi.product = null where oi.product.id = :productId")
    int detachProduct(@Param("productId") Long productId);
}
