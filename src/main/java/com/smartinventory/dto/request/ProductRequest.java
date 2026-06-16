package com.smartinventory.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/** Request DTO for creating a new product (by Supplier). */
@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    private String name;

    @NotBlank(message = "SKU is required")
    @Size(min = 3, max = 50, message = "SKU must be 3-50 characters")
    @Pattern(regexp = "^[A-Z0-9\\-_]+$", message = "SKU must be uppercase alphanumeric with hyphens/underscores")
    private String sku;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Invalid price format")
    private BigDecimal price;

    @DecimalMin(value = "0.01", message = "Cost price must be greater than 0")
    private BigDecimal costPrice;

    @NotBlank(message = "Category is required")
    @Size(max = 100)
    private String category;

    @Size(max = 50)
    private String brand;

    @Size(max = 50)
    private String unit;

    @Min(value = 0, message = "Min stock level cannot be negative")
    private Integer minStockLevel;
}
