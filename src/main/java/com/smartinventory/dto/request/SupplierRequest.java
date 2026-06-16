package com.smartinventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupplierRequest {

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 100)
    private String companyName;

    @Size(max = 100)
    private String contactPerson;

    @Size(max = 200)
    private String address;

    @Size(max = 50)
    private String city;

    @Size(max = 50)
    private String country;

    @Size(max = 50)
    private String taxId;
}
