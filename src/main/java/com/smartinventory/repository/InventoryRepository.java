package com.smartinventory.repository;

import com.smartinventory.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    List<Inventory> findByProductId(Long productId);

    List<Inventory> findByWarehouseId(Long warehouseId);

    Page<Inventory> findByWarehouseId(Long warehouseId, Pageable pageable);

    @Query("SELECT i FROM Inventory i WHERE i.quantityAvailable <= i.reorderPoint")
    List<Inventory> findLowStockItems();

    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.quantityAvailable <= i.reorderPoint")
    long countLowStockItems();

    @Query("SELECT COALESCE(SUM(i.quantityAvailable), 0) FROM Inventory i")
    Long sumAvailableStock();

    @Query("SELECT i.product.category, COALESCE(SUM(i.quantityAvailable), 0), COALESCE(SUM(i.quantityReserved), 0) " +
            "FROM Inventory i GROUP BY i.product.category ORDER BY i.product.category")
    List<Object[]> stockByCategory();

    @Query("SELECT i.warehouse.id, i.warehouse.name, COUNT(i.id), COALESCE(SUM(i.quantityAvailable), 0) " +
            "FROM Inventory i GROUP BY i.warehouse.id, i.warehouse.name")
    List<Object[]> warehousePerformance();

    @Query("SELECT SUM(i.quantityAvailable) FROM Inventory i WHERE i.product.id = :productId")
    Optional<Integer> getTotalStockForProduct(@Param("productId") Long productId);

    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId AND i.quantityAvailable >= :requiredQuantity")
    List<Inventory> findWarehousesWithSufficientStock(@Param("productId") Long productId,
                                                       @Param("requiredQuantity") int requiredQuantity);
}
