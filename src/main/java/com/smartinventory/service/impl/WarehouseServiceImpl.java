package com.smartinventory.service.impl;

import com.smartinventory.dto.request.WarehouseRequest;
import com.smartinventory.dto.response.WarehouseResponse;
import com.smartinventory.entity.Warehouse;
import com.smartinventory.exception.BusinessException;
import com.smartinventory.exception.DuplicateResourceException;
import com.smartinventory.exception.ResourceNotFoundException;
import com.smartinventory.repository.InventoryRepository;
import com.smartinventory.repository.WarehouseRepository;
import com.smartinventory.service.WarehouseService;
import com.smartinventory.util.AppUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final AppUtils appUtils;

    @Override
    @Transactional
    public WarehouseResponse createWarehouse(WarehouseRequest request) {
        if (warehouseRepository.existsByName(request.getName()))
            throw new DuplicateResourceException("Warehouse with name '" + request.getName() + "' already exists");

        Warehouse warehouse = Warehouse.builder()
                .name(request.getName())
                .location(request.getLocation())
                .description(request.getDescription())
                .managerName(request.getManagerName())
                .contactNumber(request.getContactNumber())
                .active(true)
                .build();

        warehouse = warehouseRepository.save(warehouse);
        log.info("Warehouse '{}' created by {}", warehouse.getName(), appUtils.getCurrentUsername());
        return mapToResponse(warehouse);
    }

    @Override
    @Transactional
    public WarehouseResponse updateWarehouse(Long id, WarehouseRequest request) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", id));

        if (!warehouse.getName().equals(request.getName()) && warehouseRepository.existsByName(request.getName()))
            throw new DuplicateResourceException("Warehouse name '" + request.getName() + "' already exists");

        warehouse.setName(request.getName());
        warehouse.setLocation(request.getLocation());
        warehouse.setDescription(request.getDescription());
        warehouse.setManagerName(request.getManagerName());
        warehouse.setContactNumber(request.getContactNumber());

        warehouse = warehouseRepository.save(warehouse);
        log.info("Warehouse {} updated", id);
        return mapToResponse(warehouse);
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouseById(Long id) {
        return mapToResponse(warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WarehouseResponse> getAllWarehouses(Pageable pageable) {
        return warehouseRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public WarehouseResponse toggleWarehouseStatus(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", id));
        warehouse.setActive(!warehouse.isActive());
        warehouse = warehouseRepository.save(warehouse);
        log.info("Warehouse {} status toggled to active={}", id, warehouse.isActive());
        return mapToResponse(warehouse);
    }

    @Override
    @Transactional
    public void deleteWarehouse(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", id));

        boolean hasInventory = !inventoryRepository.findByWarehouseId(id).isEmpty();
        if (hasInventory)
            throw new BusinessException("Cannot delete warehouse with existing inventory. Deactivate it instead.");

        warehouseRepository.delete(warehouse);
        log.info("Warehouse {} deleted", id);
    }

    private WarehouseResponse mapToResponse(Warehouse warehouse) {
        return WarehouseResponse.builder()
                .id(warehouse.getId())
                .name(warehouse.getName())
                .location(warehouse.getLocation())
                .description(warehouse.getDescription())
                .managerName(warehouse.getManagerName())
                .contactNumber(warehouse.getContactNumber())
                .active(warehouse.isActive())
                .createdAt(warehouse.getCreatedAt())
                .updatedAt(warehouse.getUpdatedAt())
                .build();
    }
}
