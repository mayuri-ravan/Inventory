package com.smartinventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WarehouseRequest {

    @NotBlank(message = "Warehouse name is required")
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank(message = "Location is required")
    @Size(max = 200)
    private String location;

    @Size(max = 500)
    private String description;

    @Size(max = 100)
    private String managerName;

    @Size(max = 20)
    private String contactNumber;
}
