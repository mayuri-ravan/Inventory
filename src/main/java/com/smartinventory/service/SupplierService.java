package com.smartinventory.service;

import com.smartinventory.dto.request.SupplierRequest;
import com.smartinventory.dto.response.SupplierResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupplierService {
    SupplierResponse createSupplierProfile(SupplierRequest request);
    SupplierResponse updateSupplierProfile(Long id, SupplierRequest request);
    SupplierResponse getSupplierById(Long id);
    SupplierResponse getCurrentSupplierProfile();
    Page<SupplierResponse> getAllSuppliers(String keyword, Pageable pageable);
    SupplierResponse toggleSupplierStatus(Long id);
}
