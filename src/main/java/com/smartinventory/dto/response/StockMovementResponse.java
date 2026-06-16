package com.smartinventory.dto.response;

import com.smartinventory.enums.StockMovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Long warehouseId;
    private String warehouseName;
    private StockMovementType movementType;
    private Integer quantity;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private Long referenceId;
    private String referenceType;
    private String remarks;
    private LocalDateTime movementDate;
    private String createdBy;
}
