package com.smartinventory.repository;

import com.smartinventory.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByUserId(Long userId);

    Optional<Supplier> findByUserUsername(String username);

    boolean existsByUserId(Long userId);

    @Query("SELECT s FROM Supplier s WHERE s.active = true AND " +
           "(:keyword IS NULL OR LOWER(s.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Supplier> searchActiveSuppliers(@Param("keyword") String keyword, Pageable pageable);

    Page<Supplier> findByActive(boolean active, Pageable pageable);
}
