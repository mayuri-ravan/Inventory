package com.smartinventory.repository;

import com.smartinventory.entity.StockMovement;
import com.smartinventory.enums.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findByProductId(Long productId, Pageable pageable);

    Page<StockMovement> findByWarehouseId(Long warehouseId, Pageable pageable);

    @Query("SELECT sm FROM StockMovement sm WHERE " +
           "(:productId IS NULL OR sm.product.id = :productId) AND " +
           "(:warehouseId IS NULL OR sm.warehouse.id = :warehouseId) AND " +
           "(:type IS NULL OR sm.movementType = :type) AND " +
           "(:from IS NULL OR sm.movementDate >= :from) AND " +
           "(:to IS NULL OR sm.movementDate <= :to) " +
           "ORDER BY sm.movementDate DESC")
    Page<StockMovement> searchMovements(@Param("productId") Long productId,
                                        @Param("warehouseId") Long warehouseId,
                                        @Param("type") StockMovementType type,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to,
                                        Pageable pageable);
    
    @Query("""
    	    SELECT COALESCE(SUM(sm.quantity * p.price), 0)
    	    FROM StockMovement sm
    	    JOIN sm.product p
    	    WHERE sm.movementType = com.smartinventory.enums.StockMovementType.OUTWARD
    	      AND sm.referenceType <> 'ORDER'
    	""")
    	BigDecimal getManualStockRevenue();

    	@Query("""
    	    SELECT COALESCE(SUM(sm.quantity * p.price), 0)
    	    FROM StockMovement sm
    	    JOIN sm.product p
    	    WHERE sm.movementType = com.smartinventory.enums.StockMovementType.INWARD
    	      AND sm.referenceType IN ('RETURN', 'ORDER_CANCEL')
    	""")
    	BigDecimal getReturnedRevenue();

    	@Query("""
    	    SELECT COALESCE(SUM(sm.quantity * p.price), 0)
    	    FROM StockMovement sm
    	    JOIN sm.product p
    	    WHERE sm.movementType = com.smartinventory.enums.StockMovementType.OUTWARD
    	      AND sm.movementDate >= :from
    	      AND sm.movementDate <= :to
    	      AND sm.referenceType <> 'ORDER'
    	""")
    	BigDecimal getManualStockRevenueBetween(
    	        @Param("from") LocalDateTime from,
    	        @Param("to") LocalDateTime to);

    	@Query("""
    	    SELECT COALESCE(SUM(sm.quantity * p.price), 0)
    	    FROM StockMovement sm
    	    JOIN sm.product p
    	    WHERE sm.movementType = com.smartinventory.enums.StockMovementType.INWARD
    	      AND sm.referenceType IN ('RETURN', 'ORDER_CANCEL')
    	      AND sm.movementDate >= :from
    	      AND sm.movementDate <= :to
    	""")
    	BigDecimal getReturnedRevenueBetween(
    	        @Param("from") LocalDateTime from,
    	        @Param("to") LocalDateTime to);
}
