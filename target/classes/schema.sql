-- ============================================================
-- Smart Inventory & Order Management System - SQL Schema
-- MySQL 8.0+
-- Run this script only if you prefer manual schema creation.
-- With spring.jpa.hibernate.ddl-auto=update, Hibernate auto-creates tables.
-- ============================================================

CREATE DATABASE IF NOT EXISTS smart_inventory
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE smart_inventory;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone VARCHAR(15),
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    INDEX idx_user_role (role),
    INDEX idx_user_enabled (enabled)
);

-- Suppliers table
CREATE TABLE IF NOT EXISTS suppliers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    company_name VARCHAR(100) NOT NULL,
    contact_person VARCHAR(100),
    address VARCHAR(200),
    city VARCHAR(50),
    country VARCHAR(50),
    tax_id VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_supplier_active (active)
);

-- Warehouses table
CREATE TABLE IF NOT EXISTS warehouses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    location VARCHAR(200) NOT NULL,
    description TEXT,
    manager_name VARCHAR(100),
    contact_number VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

-- Products table
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    sku VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(500),
    price DECIMAL(10,2) NOT NULL,
    cost_price DECIMAL(10,2),
    category VARCHAR(100),
    brand VARCHAR(50),
    unit VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_APPROVAL',
    min_stock_level INT DEFAULT 0,
    supplier_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    INDEX idx_product_status (status),
    INDEX idx_product_category (category)
);

-- Inventory table
CREATE TABLE IF NOT EXISTS inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    quantity_available INT NOT NULL DEFAULT 0,
    quantity_reserved INT NOT NULL DEFAULT 0,
    reorder_point INT DEFAULT 10,
    max_stock_level INT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    UNIQUE KEY uk_inventory_product_warehouse (product_id, warehouse_id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

-- Stock Movements (Audit Log)
CREATE TABLE IF NOT EXISTS stock_movements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    movement_type VARCHAR(20) NOT NULL,
    quantity INT NOT NULL,
    quantity_before INT NOT NULL,
    quantity_after INT NOT NULL,
    reference_id BIGINT,
    reference_type VARCHAR(50),
    remarks VARCHAR(255),
    movement_date DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    INDEX idx_movement_product (product_id),
    INDEX idx_movement_warehouse (warehouse_id),
    INDEX idx_movement_date (movement_date)
);

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(10,2) DEFAULT 0.00,
    tax_amount DECIMAL(10,2) DEFAULT 0.00,
    shipping_amount DECIMAL(10,2) DEFAULT 0.00,
    final_amount DECIMAL(12,2) NOT NULL,
    shipping_address VARCHAR(300),
    notes VARCHAR(500),
    order_date DATETIME NOT NULL,
    delivered_date DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    FOREIGN KEY (customer_id) REFERENCES users(id),
    INDEX idx_order_customer (customer_id),
    INDEX idx_order_status (status),
    INDEX idx_order_date (order_date)
);

-- Order Items table
CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(12,2) NOT NULL,
    discount_percentage DECIMAL(5,2) DEFAULT 0.00,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id)
);
