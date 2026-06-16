package com.smartinventory.enums;

/** Enterprise purchase-order workflow states. */
public enum PurchaseStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    SENT_TO_SUPPLIER,
    SUPPLIER_ACCEPTED,
    SUPPLIER_REJECTED,
    IN_PROGRESS,
    DISPATCHED,
    DELIVERED,
    COMPLETED
}
