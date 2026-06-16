package com.smartinventory.enums;

/** Types of stock movements for warehouse audit logs. */
public enum StockMovementType {
    INWARD,      // Stock received from supplier
    OUTWARD,     // Stock dispatched for order
    ADJUSTMENT,  // Manual stock correction
    RETURN,      // Stock returned from customer
    TRANSFER     // Inter-warehouse transfer
}
