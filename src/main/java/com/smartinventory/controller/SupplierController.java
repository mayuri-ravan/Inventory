package com.smartinventory.controller;

import com.smartinventory.dto.request.SupplierRequest;
import com.smartinventory.dto.response.ApiResponse;
import com.smartinventory.dto.response.SupplierResponse;
import com.smartinventory.service.SupplierService;
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
@RequestMapping("/suppliers")
@RequiredArgsConstructor
@Tag(name = "Suppliers", description = "Supplier profile management")
@SecurityRequirement(name = "bearerAuth")
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping("/profile")
    @PreAuthorize("hasRole('SUPPLIER')")
    @Operation(summary = "Supplier: Create supplier profile")
    public ResponseEntity<ApiResponse<SupplierResponse>> createProfile(@Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Supplier profile created", supplierService.createSupplierProfile(request)));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('SUPPLIER')")
    @Operation(summary = "Supplier: Get own profile")
    public ResponseEntity<ApiResponse<SupplierResponse>> getMyProfile() {
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", supplierService.getCurrentSupplierProfile()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    @Operation(summary = "Update supplier profile")
    public ResponseEntity<ApiResponse<SupplierResponse>> updateProfile(
            @PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated", supplierService.updateSupplierProfile(id, request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get supplier by ID")
    public ResponseEntity<ApiResponse<SupplierResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Supplier fetched", supplierService.getSupplierById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "List all suppliers")
    public ResponseEntity<ApiResponse<Page<SupplierResponse>>> getAllSuppliers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success("Suppliers fetched", supplierService.getAllSuppliers(keyword, pageable)));
    }

    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Toggle supplier active status")
    public ResponseEntity<ApiResponse<SupplierResponse>> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Supplier status toggled", supplierService.toggleSupplierStatus(id)));
    }
}
