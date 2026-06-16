package com.smartinventory.controller;

import com.smartinventory.dto.request.PurchaseActionRequest;
import com.smartinventory.dto.request.PurchaseOrderRequest;
import com.smartinventory.dto.response.ApiResponse;
import com.smartinventory.dto.response.PurchaseOrderResponse;
import com.smartinventory.enums.PurchaseStatus;
import com.smartinventory.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/purchase-orders")
@RequiredArgsConstructor
@Tag(name = "Purchase Orders")
@SecurityRequirement(name = "bearerAuth")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Create purchase order or purchase request")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> create(@Valid @RequestBody PurchaseOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Purchase order created", purchaseOrderService.create(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF', 'SUPPLIER')")
    @Operation(summary = "List purchase orders with warehouse/supplier scoping")
    public ResponseEntity<ApiResponse<Page<PurchaseOrderResponse>>> search(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) PurchaseStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success("Purchase orders fetched",
                purchaseOrderService.search(warehouseId, supplierId, status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF', 'SUPPLIER')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Purchase order fetched", purchaseOrderService.getById(id)));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> approve(
            @PathVariable Long id, @RequestBody(required = false) PurchaseActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Purchase order approved",
                purchaseOrderService.approve(id, request == null ? new PurchaseActionRequest() : request)));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> reject(
            @PathVariable Long id, @RequestBody(required = false) PurchaseActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Purchase order rejected",
                purchaseOrderService.reject(id, request == null ? new PurchaseActionRequest() : request)));
    }

    @PutMapping("/{id}/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> send(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Purchase order sent to supplier",
                purchaseOrderService.sendToSupplier(id)));
    }

    @PutMapping("/{id}/supplier-response")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> supplierResponse(
            @PathVariable Long id,
            @RequestParam boolean accepted,
            @RequestBody(required = false) PurchaseActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Supplier response saved",
                purchaseOrderService.supplierResponse(id, accepted,
                        request == null ? new PurchaseActionRequest() : request)));
    }

    @PutMapping("/{id}/dispatch")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPPLIER')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> dispatch(
            @PathVariable Long id, @RequestBody(required = false) PurchaseActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Purchase order dispatched",
                purchaseOrderService.dispatch(id, request == null ? new PurchaseActionRequest() : request)));
    }

    @PutMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> receive(
            @PathVariable Long id, @RequestBody(required = false) PurchaseActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Goods received and inventory updated",
                purchaseOrderService.receive(id, request == null ? new PurchaseActionRequest() : request)));
    }
}
