package com.smartinventory.entity;

import com.smartinventory.enums.StockMovementType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Immutable audit log for every stock movement (inward/outward/adjustment).
 * Provides a full history of inventory changes.
 */
@Entity
@Table(name = "stock_movements", indexes = {
        @Index(name = "idx_stock_movement_product", columnList = "product_id"),
        @Index(name = "idx_stock_movement_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_stock_movement_date", columnList = "movement_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private StockMovementType movementType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "quantity_before", nullable = false)
    private Integer quantityBefore;

    @Column(name = "quantity_after", nullable = false)
    private Integer quantityAfter;

    @Column(name = "reference_id")
    private Long referenceId; // Order ID or PO ID that caused this movement

    @Column(name = "reference_type", length = 50)
    private String referenceType; // "ORDER", "PURCHASE_ORDER", "MANUAL"

    @Column(length = 255)
    private String remarks;

    @Column(name = "movement_date", nullable = false)
    @Builder.Default
    private LocalDateTime movementDate = LocalDateTime.now();
}
