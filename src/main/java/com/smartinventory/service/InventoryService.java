package com.smartinventory.service;

import com.smartinventory.dto.request.StockUpdateRequest;
import com.smartinventory.dto.response.InventoryResponse;
import com.smartinventory.dto.response.StockMovementResponse;
import com.smartinventory.enums.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryService {
    InventoryResponse updateStock(StockUpdateRequest request);
    InventoryResponse getInventoryByProductAndWarehouse(Long productId, Long warehouseId);
    List<InventoryResponse> getInventoryByProduct(Long productId);
    Page<InventoryResponse> getInventoryByWarehouse(Long warehouseId, Pageable pageable);
    List<InventoryResponse> getLowStockItems();
    Page<StockMovementResponse> getStockMovements(Long productId, Long warehouseId,
            StockMovementType type, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
