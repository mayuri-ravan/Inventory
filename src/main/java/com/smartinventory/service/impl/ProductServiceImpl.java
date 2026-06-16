package com.smartinventory.service.impl;

import com.smartinventory.dto.request.ProductRequest;
import com.smartinventory.dto.response.ProductResponse;
import com.smartinventory.entity.Product;
import com.smartinventory.entity.Supplier;
import com.smartinventory.enums.ProductStatus;
import com.smartinventory.exception.BusinessException;
import com.smartinventory.exception.DuplicateResourceException;
import com.smartinventory.exception.ResourceNotFoundException;
import com.smartinventory.repository.InventoryRepository;
import com.smartinventory.repository.ProductRepository;
import com.smartinventory.repository.SupplierRepository;
import com.smartinventory.service.ProductService;
import com.smartinventory.util.AppUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryRepository inventoryRepository;
    private final AppUtils appUtils;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku()))
            throw new DuplicateResourceException("Product with SKU '" + request.getSku() + "' already exists");

        Supplier supplier = supplierRepository.findByUserId(appUtils.getCurrentUser().getId())
                .orElseThrow(() -> new BusinessException("Supplier profile not found. Please create a supplier profile first."));

        if (!supplier.isActive())
            throw new BusinessException("Your supplier account is inactive. Contact admin.");

        Product product = Product.builder()
                .name(request.getName())
                .sku(request.getSku())
                .description(request.getDescription())
                .price(request.getPrice())
                .costPrice(request.getCostPrice())
                .category(request.getCategory())
                .brand(request.getBrand())
                .unit(request.getUnit())
                .minStockLevel(request.getMinStockLevel() != null ? request.getMinStockLevel() : 0)
                .status(ProductStatus.PENDING_APPROVAL)
                .supplier(supplier)
                .build();

        product = productRepository.save(product);
        log.info("Product '{}' (SKU: {}) submitted by supplier '{}'",
                product.getName(), product.getSku(), supplier.getCompanyName());
        return mapToResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        // Supplier can only update their own products
        Supplier supplier = supplierRepository.findByUserId(appUtils.getCurrentUser().getId())
                .orElse(null);
        if (supplier != null && !product.getSupplier().getId().equals(supplier.getId()))
            throw new BusinessException("You can only update your own products");

        // SKU uniqueness check (allow same SKU for same product)
        if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku()))
            throw new DuplicateResourceException("SKU '" + request.getSku() + "' is already used");

        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCostPrice(request.getCostPrice());
        product.setCategory(request.getCategory());
        product.setBrand(request.getBrand());
        product.setUnit(request.getUnit());
        if (request.getMinStockLevel() != null) product.setMinStockLevel(request.getMinStockLevel());

        // Re-submit for approval if already approved
        if (product.getStatus() == ProductStatus.APPROVED) {
            product.setStatus(ProductStatus.PENDING_APPROVAL);
            log.info("Product {} re-submitted for approval after update", id);
        }

        product = productRepository.save(product);
        log.info("Product {} updated", id);
        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return mapToResponse(productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(ProductStatus status, String category, String keyword, Pageable pageable) {
        return productRepository.searchProducts(status, category, keyword, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public ProductResponse approveProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        if (product.getStatus() == ProductStatus.APPROVED)
            throw new BusinessException("Product is already approved");

        product.setStatus(ProductStatus.APPROVED);
        product = productRepository.save(product);
        log.info("Product {} '{}' approved by {}", id, product.getName(), appUtils.getCurrentUsername());
        return mapToResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse rejectProduct(Long id, String reason) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        product.setStatus(ProductStatus.REJECTED);
        // Append reason to description for audit trail
        if (reason != null && !reason.isBlank()) {
            product.setDescription((product.getDescription() != null ? product.getDescription() : "")
                    + " [REJECTED: " + reason + "]");
        }
        product = productRepository.save(product);
        log.info("Product {} rejected by {}: {}", id, appUtils.getCurrentUsername(), reason);
        return mapToResponse(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        product.setStatus(ProductStatus.DISCONTINUED);
        productRepository.save(product);
        log.info("Product {} discontinued", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return productRepository.findAllCategories();
    }

    private ProductResponse mapToResponse(Product product) {
        Integer totalStock = inventoryRepository.getTotalStockForProduct(product.getId()).orElse(0);
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .description(product.getDescription())
                .price(product.getPrice())
                .costPrice(product.getCostPrice())
                .category(product.getCategory())
                .brand(product.getBrand())
                .unit(product.getUnit())
                .status(product.getStatus())
                .minStockLevel(product.getMinStockLevel())
                .supplierId(product.getSupplier().getId())
                .supplierName(product.getSupplier().getCompanyName())
                .totalStock(totalStock)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
