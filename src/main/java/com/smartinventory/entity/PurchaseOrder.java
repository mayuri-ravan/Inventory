package com.smartinventory.entity;

import com.smartinventory.enums.PurchaseStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Purchase order with manager/admin/supplier approval workflow. */
@Entity
@Table(name = "purchase_orders", indexes = {
        @Index(name = "idx_po_number", columnList = "purchase_order_number", unique = true),
        @Index(name = "idx_po_status", columnList = "status"),
        @Index(name = "idx_po_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_po_supplier", columnList = "supplier_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_order_number", nullable = false, unique = true, length = 50)
    private String purchaseOrderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PurchaseStatus status = PurchaseStatus.DRAFT;

    @Column(name = "manager_approval_required", nullable = false)
    @Builder.Default
    private boolean managerApprovalRequired = true;

    @Column(name = "admin_approval_required", nullable = false)
    @Builder.Default
    private boolean adminApprovalRequired = false;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 700)
    private String notes;

    @Column(name = "supplier_notes", length = 700)
    private String supplierNotes;

    @Column(name = "expected_delivery_date")
    private LocalDateTime expectedDeliveryDate;

    @Column(name = "delivered_date")
    private LocalDateTime deliveredDate;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseOrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<ApprovalRecord> approvals = new ArrayList<>();

    public void recalculateTotal() {
        this.totalAmount = items.stream()
                .map(PurchaseOrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
