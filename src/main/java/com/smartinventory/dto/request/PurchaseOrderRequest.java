package com.smartinventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class PurchaseOrderRequest {

    @NotNull(message = "Warehouse is required")
    private Long warehouseId;

    @NotNull(message = "Supplier is required")
    private Long supplierId;

    private boolean adminApprovalRequired;

    private LocalDateTime expectedDeliveryDate;

    @Size(max = 700)
    private String notes;

    @Valid
    @NotEmpty(message = "At least one item is required")
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        @NotNull(message = "Product is required")
        private Long productId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be greater than zero")
        private Integer quantity;

        @NotNull(message = "Unit cost is required")
        private BigDecimal unitCost;
    }
}
