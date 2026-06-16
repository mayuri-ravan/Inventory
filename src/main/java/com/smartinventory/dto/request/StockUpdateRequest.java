package com.smartinventory.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/** Request DTO for updating stock (inward/outward/adjustment). */
@Data
public class StockUpdateRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotBlank(message = "Movement type is required")
    private String movementType; // INWARD, OUTWARD, ADJUSTMENT

    @Size(max = 255)
    private String remarks;

    private Long referenceId;
    private String referenceType;
}
