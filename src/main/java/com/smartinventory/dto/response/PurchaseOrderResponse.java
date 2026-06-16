package com.smartinventory.dto.response;

import com.smartinventory.enums.PurchaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderResponse {
    private Long id;
    private String purchaseOrderNumber;
    private Long warehouseId;
    private String warehouseName;
    private Long supplierId;
    private String supplierName;
    private Long requestedById;
    private String requestedByName;
    private PurchaseStatus status;
    private boolean managerApprovalRequired;
    private boolean adminApprovalRequired;
    private BigDecimal totalAmount;
    private String notes;
    private String supplierNotes;
    private LocalDateTime expectedDeliveryDate;
    private LocalDateTime deliveredDate;
    private LocalDateTime createdAt;
    @Builder.Default
    private List<ItemResponse> items = new ArrayList<>();
    @Builder.Default
    private List<ApprovalResponse> approvals = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private String productSku;
        private Integer quantity;
        private BigDecimal unitCost;
        private BigDecimal lineTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalResponse {
        private String actorName;
        private String actorRole;
        private PurchaseStatus status;
        private String comments;
        private LocalDateTime createdAt;
    }
}
