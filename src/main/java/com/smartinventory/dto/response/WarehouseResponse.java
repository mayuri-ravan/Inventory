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
public class WarehouseResponse {
    private Long id;
    private String name;
    private String location;
    private String description;
    private String managerName;
    private String contactNumber;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
