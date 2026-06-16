package com.smartinventory.repository;

import com.smartinventory.entity.PurchaseOrder;
import com.smartinventory.enums.PurchaseStatus;
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
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findByPurchaseOrderNumber(String purchaseOrderNumber);

    @Query("SELECT po FROM PurchaseOrder po WHERE " +
            "(:warehouseId IS NULL OR po.warehouse.id = :warehouseId) AND " +
            "(:supplierId IS NULL OR po.supplier.id = :supplierId) AND " +
            "(:status IS NULL OR po.status = :status)")
    Page<PurchaseOrder> search(@Param("warehouseId") Long warehouseId,
                               @Param("supplierId") Long supplierId,
                               @Param("status") PurchaseStatus status,
                               Pageable pageable);

    long countByStatus(PurchaseStatus status);

    @Query("SELECT COALESCE(SUM(po.totalAmount), 0) FROM PurchaseOrder po WHERE " +
            "po.status IN (com.smartinventory.enums.PurchaseStatus.DELIVERED, com.smartinventory.enums.PurchaseStatus.COMPLETED)")
    BigDecimal sumDeliveredPurchases();

    @Query("SELECT COALESCE(SUM(po.totalAmount), 0) FROM PurchaseOrder po WHERE " +
            "po.createdAt >= :from AND po.createdAt <= :to")
    BigDecimal sumPurchasesBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<PurchaseOrder> findTop10ByOrderByCreatedAtDesc();
}
