package com.smartinventory;

import org.springframework.boot.SpringApplication;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Smart Inventory & Order Management System.
 *
 * <p>This enterprise application provides:
 * <ul>
 *   <li>Role-based access control (ADMIN, MANAGER, STAFF, SUPPLIER, CUSTOMER)</li>
 *   <li>Full inventory and stock management</li>
 *   <li>Order lifecycle management</li>
 *   <li>Supplier and product management</li>
 *   <li>Warehouse operations and audit logging</li>
 * </ul>
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class SmartInventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartInventoryApplication.class, args);
    }
}
