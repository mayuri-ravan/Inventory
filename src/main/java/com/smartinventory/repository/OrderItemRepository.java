package com.smartinventory.repository;

import com.smartinventory.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT oi.product.id, oi.product.name, oi.product.sku, SUM(oi.quantity), SUM(oi.totalPrice) " +
            "FROM OrderItem oi GROUP BY oi.product.id, oi.product.name, oi.product.sku " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> topSelling(Pageable pageable);

    @Query("SELECT oi.product.id, oi.product.name, oi.product.sku, SUM(oi.quantity), SUM(oi.totalPrice) " +
            "FROM OrderItem oi GROUP BY oi.product.id, oi.product.name, oi.product.sku " +
            "ORDER BY SUM(oi.quantity) ASC")
    List<Object[]> slowMoving(Pageable pageable);
}
