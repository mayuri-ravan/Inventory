package com.smartinventory.controller;

import com.smartinventory.dto.request.StockUpdateRequest;
import com.smartinventory.dto.response.ApiResponse;
import com.smartinventory.dto.response.InventoryResponse;
import com.smartinventory.dto.response.StockMovementResponse;
import com.smartinventory.enums.StockMovementType;
import com.smartinventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Stock management and movement tracking")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Update stock (INWARD/OUTWARD/ADJUSTMENT)")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateStock(@Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Stock updated", inventoryService.updateStock(request)));
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Get inventory for a product across all warehouses")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success("Inventory fetched", inventoryService.getInventoryByProduct(productId)));
    }

    @GetMapping("/warehouse/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Get all inventory in a warehouse")
    public ResponseEntity<ApiResponse<Page<InventoryResponse>>> getByWarehouse(
            @PathVariable Long warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success("Inventory fetched",
                inventoryService.getInventoryByWarehouse(warehouseId, pageable)));
    }

    @GetMapping("/product/{productId}/warehouse/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Get inventory for a specific product in a specific warehouse")
    public ResponseEntity<ApiResponse<InventoryResponse>> getByProductAndWarehouse(
            @PathVariable Long productId, @PathVariable Long warehouseId) {
        return ResponseEntity.ok(ApiResponse.success("Inventory fetched",
                inventoryService.getInventoryByProductAndWarehouse(productId, warehouseId)));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Get all items at or below reorder point (low stock alert)")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getLowStock() {
        return ResponseEntity.ok(ApiResponse.success("Low stock items fetched", inventoryService.getLowStockItems()));
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Get stock movement history (audit log)")
    public ResponseEntity<ApiResponse<Page<StockMovementResponse>>> getMovements(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) StockMovementType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success("Movements fetched",
                inventoryService.getStockMovements(productId, warehouseId, type, from, to, pageable)));
    }
}
