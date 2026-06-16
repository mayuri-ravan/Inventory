package com.smartinventory.dto.response;

import com.smartinventory.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private String sku;
    private String description;
    private BigDecimal price;
    private BigDecimal costPrice;
    private String category;
    private String brand;
    private String unit;
    private ProductStatus status;
    private Integer minStockLevel;
    private Long supplierId;
    private String supplierName;
    private Integer totalStock; // Aggregated from all warehouses
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
