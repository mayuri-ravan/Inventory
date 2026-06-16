package com.smartinventory.service.impl;

import com.smartinventory.dto.request.SupplierRequest;
import com.smartinventory.dto.response.SupplierResponse;
import com.smartinventory.entity.Supplier;
import com.smartinventory.entity.User;
import com.smartinventory.exception.BusinessException;
import com.smartinventory.exception.DuplicateResourceException;
import com.smartinventory.exception.ResourceNotFoundException;
import com.smartinventory.repository.SupplierRepository;
import com.smartinventory.service.SupplierService;
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
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final AppUtils appUtils;

    @Override
    @Transactional
    public SupplierResponse createSupplierProfile(SupplierRequest request) {
        User currentUser = appUtils.getCurrentUser();
        if (supplierRepository.existsByUserId(currentUser.getId()))
            throw new DuplicateResourceException("Supplier profile already exists for this user");

        Supplier supplier = Supplier.builder()
                .user(currentUser)
                .companyName(request.getCompanyName())
                .contactPerson(request.getContactPerson())
                .address(request.getAddress())
                .city(request.getCity())
                .country(request.getCountry())
                .taxId(request.getTaxId())
                .active(true)
                .build();

        supplier = supplierRepository.save(supplier);
        log.info("Supplier profile created for user '{}'", currentUser.getUsername());
        return mapToResponse(supplier);
    }

    @Override
    @Transactional
    public SupplierResponse updateSupplierProfile(Long id, SupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));

        // Only owner or admin can update
        User currentUser = appUtils.getCurrentUser();
        if (!supplier.getUser().getId().equals(currentUser.getId())
                && !currentUser.getRole().name().equals("ROLE_ADMIN")) {
            throw new BusinessException("You can only update your own supplier profile");
        }

        supplier.setCompanyName(request.getCompanyName());
        supplier.setContactPerson(request.getContactPerson());
        supplier.setAddress(request.getAddress());
        supplier.setCity(request.getCity());
        supplier.setCountry(request.getCountry());
        supplier.setTaxId(request.getTaxId());

        supplier = supplierRepository.save(supplier);
        log.info("Supplier profile {} updated", id);
        return mapToResponse(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getSupplierById(Long id) {
        return mapToResponse(supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getCurrentSupplierProfile() {
        User currentUser = appUtils.getCurrentUser();
        return mapToResponse(supplierRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier profile not found. Please create one first.")));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierResponse> getAllSuppliers(String keyword, Pageable pageable) {
        return supplierRepository.searchActiveSuppliers(keyword, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public SupplierResponse toggleSupplierStatus(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));
        supplier.setActive(!supplier.isActive());
        supplier = supplierRepository.save(supplier);
        log.info("Supplier {} status toggled to active={}", id, supplier.isActive());
        return mapToResponse(supplier);
    }

    private SupplierResponse mapToResponse(Supplier supplier) {
        return SupplierResponse.builder()
                .id(supplier.getId())
                .userId(supplier.getUser().getId())
                .username(supplier.getUser().getUsername())
                .email(supplier.getUser().getEmail())
                .companyName(supplier.getCompanyName())
                .contactPerson(supplier.getContactPerson())
                .address(supplier.getAddress())
                .city(supplier.getCity())
                .country(supplier.getCountry())
                .taxId(supplier.getTaxId())
                .active(supplier.isActive())
                .createdAt(supplier.getCreatedAt())
                .build();
    }
}
