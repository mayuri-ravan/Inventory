package com.smartinventory.service;

import com.smartinventory.dto.request.ProductRequest;
import com.smartinventory.dto.response.ProductResponse;
import com.smartinventory.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);
    ProductResponse updateProduct(Long id, ProductRequest request);
    ProductResponse getProductById(Long id);
    Page<ProductResponse> getAllProducts(ProductStatus status, String category, String keyword, Pageable pageable);
    ProductResponse approveProduct(Long id);
    ProductResponse rejectProduct(Long id, String reason);
    void deleteProduct(Long id);
    List<String> getAllCategories();
}
