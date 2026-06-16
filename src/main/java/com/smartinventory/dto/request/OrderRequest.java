package com.smartinventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** Request DTO for placing a new order. */
@Data
public class OrderRequest {

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequest> items;

    @Size(max = 300, message = "Shipping address cannot exceed 300 characters")
    private String shippingAddress;

    @Size(max = 500)
    private String notes;

    /** Inner DTO for each line item */
    @Data
    public static class OrderItemRequest {

        @NotNull(message = "Product ID is required")
        @Positive(message = "Product ID must be positive")
        private Long productId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 1000, message = "Quantity cannot exceed 1000 per item")
        private Integer quantity;

        @DecimalMin(value = "0.00", message = "Discount percentage cannot be negative")
        @DecimalMax(value = "100.00", message = "Discount percentage cannot exceed 100")
        private BigDecimal discountPercentage;
    }
}
