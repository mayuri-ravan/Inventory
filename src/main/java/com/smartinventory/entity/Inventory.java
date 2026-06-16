package com.smartinventory.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Tracks stock levels for a specific product in a specific warehouse.
 */
@Entity
@Table(name = "inventory",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_inventory_product_warehouse",
                columnNames = {"product_id", "warehouse_id"}
        ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "quantity_available", nullable = false)
    @Builder.Default
    private Integer quantityAvailable = 0;

    @Column(name = "quantity_reserved", nullable = false)
    @Builder.Default
    private Integer quantityReserved = 0;

    @Column(name = "reorder_point")
    @Builder.Default
    private Integer reorderPoint = 10;

    @Column(name = "max_stock_level")
    private Integer maxStockLevel;

    /** Returns total physical stock (available + reserved) */
    public Integer getTotalStock() {
        return quantityAvailable + quantityReserved;
    }

    /** Deducts stock safely; throws exception if insufficient */
    public void deductStock(int quantity) {
        if (quantityAvailable < quantity) {
            throw new IllegalStateException(
                    "Insufficient stock. Available: " + quantityAvailable + ", Requested: " + quantity);
        }
        this.quantityAvailable -= quantity;
    }

    /** Adds stock (inward movement) */
    public void addStock(int quantity) {
        this.quantityAvailable += quantity;
    }
}
