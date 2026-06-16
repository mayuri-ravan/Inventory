package com.smartinventory.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PurchaseActionRequest {
    @Size(max = 700)
    private String comments;

    @Size(max = 700)
    private String supplierNotes;
}
