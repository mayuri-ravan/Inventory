package com.smartinventory.service.impl;

import com.smartinventory.dto.request.StockUpdateRequest;
import com.smartinventory.dto.response.InventoryResponse;
import com.smartinventory.dto.response.StockMovementResponse;
import com.smartinventory.entity.Inventory;
import com.smartinventory.entity.Product;
import com.smartinventory.entity.StockMovement;
import com.smartinventory.entity.Warehouse;
import com.smartinventory.enums.ProductStatus;
import com.smartinventory.enums.StockMovementType;
import com.smartinventory.exception.BusinessException;
import com.smartinventory.exception.ResourceNotFoundException;
import com.smartinventory.repository.InventoryRepository;
import com.smartinventory.repository.ProductRepository;
import com.smartinventory.repository.StockMovementRepository;
import com.smartinventory.repository.WarehouseRepository;
import com.smartinventory.service.InventoryService;
import com.smartinventory.util.AppUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockMovementRepository stockMovementRepository;
    private final AppUtils appUtils;

    @Override
    @Transactional
    public InventoryResponse updateStock(StockUpdateRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", request.getWarehouseId()));

        if (product.getStatus() != ProductStatus.APPROVED)
            throw new BusinessException("Only approved products can have stock updates");
        if (!warehouse.isActive())
            throw new BusinessException("Cannot update stock for an inactive warehouse");

        StockMovementType movementType = StockMovementType.valueOf(request.getMovementType());

        Inventory inventory = inventoryRepository
                .findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseGet(() -> Inventory.builder()
                        .product(product)
                        .warehouse(warehouse)
                        .quantityAvailable(0)
                        .quantityReserved(0)
                        .reorderPoint(10)
                        .build());

        int quantityBefore = inventory.getQuantityAvailable();

        switch (movementType) {
            case INWARD -> inventory.addStock(request.getQuantity());
            case OUTWARD -> inventory.deductStock(request.getQuantity());
            case ADJUSTMENT -> inventory.setQuantityAvailable(request.getQuantity());
            default -> throw new BusinessException("Unsupported movement type: " + movementType);
        }

        inventory = inventoryRepository.save(inventory);

        // Record stock movement for audit trail
        StockMovement movement = StockMovement.builder()
                .product(product)
                .warehouse(warehouse)
                .movementType(movementType)
                .quantity(request.getQuantity())
                .quantityBefore(quantityBefore)
                .quantityAfter(inventory.getQuantityAvailable())
                .referenceId(request.getReferenceId())
                .referenceType(request.getReferenceType() != null ? request.getReferenceType() : "MANUAL")
                .remarks(request.getRemarks())
                .movementDate(LocalDateTime.now())
                .build();
        stockMovementRepository.save(movement);

        log.info("Stock {} for product '{}' in warehouse '{}': {} -> {}",
                movementType, product.getName(), warehouse.getName(), quantityBefore, inventory.getQuantityAvailable());

        return mapToInventoryResponse(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductAndWarehouse(Long productId, Long warehouseId) {
        return mapToInventoryResponse(inventoryRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory record not found for product " + productId + " in warehouse " + warehouseId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventoryByProduct(Long productId) {
        return inventoryRepository.findByProductId(productId).stream()
                .map(this::mapToInventoryResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> getInventoryByWarehouse(Long warehouseId, Pageable pageable) {
        return inventoryRepository.findByWarehouseId(warehouseId, pageable).map(this::mapToInventoryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStockItems() {
        return inventoryRepository.findLowStockItems().stream()
                .map(this::mapToInventoryResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockMovementResponse> getStockMovements(Long productId, Long warehouseId,
            StockMovementType type, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return stockMovementRepository.searchMovements(productId, warehouseId, type, from, to, pageable)
                .map(this::mapToMovementResponse);
    }

    private InventoryResponse mapToInventoryResponse(Inventory inv) {
        return InventoryResponse.builder()
                .id(inv.getId())
                .productId(inv.getProduct().getId())
                .productName(inv.getProduct().getName())
                .productSku(inv.getProduct().getSku())
                .warehouseId(inv.getWarehouse().getId())
                .warehouseName(inv.getWarehouse().getName())
                .quantityAvailable(inv.getQuantityAvailable())
                .quantityReserved(inv.getQuantityReserved())
                .totalStock(inv.getTotalStock())
                .reorderPoint(inv.getReorderPoint())
                .maxStockLevel(inv.getMaxStockLevel())
                .lowStock(inv.getQuantityAvailable() <= inv.getReorderPoint())
                .updatedAt(inv.getUpdatedAt())
                .build();
    }

    private StockMovementResponse mapToMovementResponse(StockMovement sm) {
        return StockMovementResponse.builder()
                .id(sm.getId())
                .productId(sm.getProduct().getId())
                .productName(sm.getProduct().getName())
                .productSku(sm.getProduct().getSku())
                .warehouseId(sm.getWarehouse().getId())
                .warehouseName(sm.getWarehouse().getName())
                .movementType(sm.getMovementType())
                .quantity(sm.getQuantity())
                .quantityBefore(sm.getQuantityBefore())
                .quantityAfter(sm.getQuantityAfter())
                .referenceId(sm.getReferenceId())
                .referenceType(sm.getReferenceType())
                .remarks(sm.getRemarks())
                .movementDate(sm.getMovementDate())
                .createdBy(sm.getCreatedBy())
                .build();
    }
}
