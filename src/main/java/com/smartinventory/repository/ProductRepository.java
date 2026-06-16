package com.smartinventory.repository;

import com.smartinventory.entity.Product;
import com.smartinventory.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    long countByStatus(ProductStatus status);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findBySupplierId(Long supplierId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:category IS NULL OR LOWER(p.category) = LOWER(:category)) AND " +
           "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchProducts(@Param("status") ProductStatus status,
                                 @Param("category") String category,
                                 @Param("keyword") String keyword,
                                 Pageable pageable);

    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL ORDER BY p.category")
    java.util.List<String> findAllCategories();

    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL AND LOWER(p.category) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY p.category")
    java.util.List<String> searchCategories(@Param("keyword") String keyword);
}
