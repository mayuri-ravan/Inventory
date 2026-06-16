package com.smartinventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierResponse {
    private Long id;
    private Long userId;
    private String username;
    private String email;
    private String companyName;
    private String contactPerson;
    private String address;
    private String city;
    private String country;
    private String taxId;
    private boolean active;
    private LocalDateTime createdAt;
}
