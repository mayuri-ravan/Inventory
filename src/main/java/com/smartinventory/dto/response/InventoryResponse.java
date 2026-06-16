package com.smartinventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Long warehouseId;
    private String warehouseName;
    private Integer quantityAvailable;
    private Integer quantityReserved;
    private Integer totalStock;
    private Integer reorderPoint;
    private Integer maxStockLevel;
    private boolean lowStock;
    private LocalDateTime updatedAt;
}
