package com.smartinventory.service;

import com.smartinventory.dto.request.WarehouseRequest;
import com.smartinventory.dto.response.WarehouseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WarehouseService {
    WarehouseResponse createWarehouse(WarehouseRequest request);
    WarehouseResponse updateWarehouse(Long id, WarehouseRequest request);
    WarehouseResponse getWarehouseById(Long id);
    Page<WarehouseResponse> getAllWarehouses(Pageable pageable);
    WarehouseResponse toggleWarehouseStatus(Long id);
    void deleteWarehouse(Long id);
}
