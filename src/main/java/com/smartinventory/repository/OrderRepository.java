package com.smartinventory.repository;

import com.smartinventory.entity.Order;
import com.smartinventory.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE " +
           "o.customer.id = :customerId AND (:status IS NULL OR o.status = :status)")
    Page<Order> findByCustomerIdAndStatus(@Param("customerId") Long customerId,
                                          @Param("status") OrderStatus status,
                                          Pageable pageable);

    @Query("SELECT o FROM Order o WHERE " +
           "(:customerId IS NULL OR o.customer.id = :customerId) AND " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:from IS NULL OR o.orderDate >= :from) AND " +
           "(:to IS NULL OR o.orderDate <= :to)")
    Page<Order> searchOrders(@Param("customerId") Long customerId,
                             @Param("status") OrderStatus status,
                             @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to,
                             Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    Long countByStatus(@Param("status") OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.status <> com.smartinventory.enums.OrderStatus.CANCELLED")
    BigDecimal sumRevenue();

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE " +
           "o.status <> com.smartinventory.enums.OrderStatus.CANCELLED AND o.orderDate >= :from AND o.orderDate <= :to")
    BigDecimal sumRevenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<Order> findTop10ByOrderByOrderDateDesc();
}
