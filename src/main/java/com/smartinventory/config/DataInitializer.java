package com.smartinventory.config;

import com.smartinventory.entity.Inventory;
import com.smartinventory.entity.Product;
import com.smartinventory.entity.Supplier;
import com.smartinventory.entity.User;
import com.smartinventory.entity.Warehouse;
import com.smartinventory.enums.ProductStatus;
import com.smartinventory.enums.Role;
import com.smartinventory.repository.InventoryRepository;
import com.smartinventory.repository.ProductRepository;
import com.smartinventory.repository.SupplierRepository;
import com.smartinventory.repository.UserRepository;
import com.smartinventory.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds the database with default admin user and a sample warehouse on first run.
 * Only runs if the admin user does not already exist.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdminUser();
        seedDefaultWarehouse();
        seedDemoUsersAndInventory();
    }

    private void seedAdminUser() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@smartinventory.com")
                    .password(passwordEncoder.encode("Admin@12345"))
                    .firstName("System")
                    .lastName("Administrator")
                    .role(Role.ROLE_ADMIN)
                    .enabled(true)
                    .accountNonLocked(true)
                    .build();
            userRepository.save(admin);
            log.info("=======================================================");
            log.info("Default ADMIN user created:");
            log.info("  Username : admin");
            log.info("  Password : Admin@12345");
            log.info("  CHANGE THIS PASSWORD IN PRODUCTION!");
            log.info("=======================================================");
        }
    }

    private void seedDefaultWarehouse() {
        if (!warehouseRepository.existsByName("Main Warehouse")) {
            Warehouse warehouse = Warehouse.builder()
                    .name("Main Warehouse")
                    .location("123 Industrial Zone, City")
                    .description("Primary storage facility")
                    .managerName("Warehouse Manager")
                    .contactNumber("+1234567890")
                    .active(true)
                    .build();
            warehouseRepository.save(warehouse);
            log.info("Default warehouse 'Main Warehouse' created");
        }
    }

    private void seedDemoUsersAndInventory() {
        Warehouse warehouse = warehouseRepository.findByName("Main Warehouse").orElseThrow();

        User manager = ensureUser("manager1", "manager1@smartinventory.com", "Manager@123",
                "Maya", "Manager", Role.ROLE_MANAGER, warehouse);
        User staff = ensureUser("staff1", "staff1@smartinventory.com", "Staff@12345",
                "Sam", "Staff", Role.ROLE_STAFF, warehouse);
        User supplierUser = ensureUser("supplier1", "supplier1@smartinventory.com", "Supplier@123",
                "Sonia", "Supplier", Role.ROLE_SUPPLIER, null);
        ensureUser("customer1", "customer1@smartinventory.com", "Customer@123",
                "Chris", "Customer", Role.ROLE_CUSTOMER, null);

        if (warehouse.getManager() == null) {
            warehouse.setManager(manager);
            warehouse.setManagerName(manager.getFullName());
            warehouseRepository.save(warehouse);
        }

        Supplier supplier = supplierRepository.findByUserId(supplierUser.getId())
                .orElseGet(() -> supplierRepository.save(Supplier.builder()
                        .user(supplierUser)
                        .companyName("Northstar Supply Co.")
                        .contactPerson(supplierUser.getFullName())
                        .address("Supplier Park, Sector 7")
                        .city("Bengaluru")
                        .country("India")
                        .taxId("GST-DEMO-001")
                        .active(true)
                        .build()));

        Product laptop = ensureProduct("Business Laptop", "ELEC-LAP-001", "Electronics",
                "Northstar", "pieces", new BigDecimal("72999.00"), new BigDecimal("61250.00"), 8, supplier);
        Product scanner = ensureProduct("Barcode Scanner", "ELEC-SCN-001", "Electronics",
                "ScanPro", "pieces", new BigDecimal("7999.00"), new BigDecimal("5200.00"), 12, supplier);
        Product labels = ensureProduct("Thermal Labels Pack", "STAT-LBL-001", "Stationery",
                "PrintMate", "packs", new BigDecimal("499.00"), new BigDecimal("220.00"), 25, supplier);

        ensureInventory(laptop, warehouse, 6, 10, 80);
        ensureInventory(scanner, warehouse, 42, 12, 120);
        ensureInventory(labels, warehouse, 18, 25, 300);

        log.info("Demo manager/staff/supplier/customer users and inventory data are ready");
    }

    private User ensureUser(String username, String email, String password, String firstName,
                            String lastName, Role role, Warehouse warehouse) {
        return userRepository.findByUsername(username).orElseGet(() -> userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .phone("+919999999999")
                .assignedWarehouse(warehouse)
                .role(role)
                .enabled(true)
                .accountNonLocked(true)
                .build()));
    }

    private Product ensureProduct(String name, String sku, String category, String brand, String unit,
                                  BigDecimal price, BigDecimal costPrice, int minStock, Supplier supplier) {
        return productRepository.findBySku(sku).orElseGet(() -> productRepository.save(Product.builder()
                .name(name)
                .sku(sku)
                .description(name + " seeded for enterprise dashboard and workflow demos")
                .price(price)
                .costPrice(costPrice)
                .category(category)
                .brand(brand)
                .unit(unit)
                .status(ProductStatus.APPROVED)
                .minStockLevel(minStock)
                .supplier(supplier)
                .build()));
    }

    private void ensureInventory(Product product, Warehouse warehouse, int quantity, int reorderPoint, int maxStock) {
        inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseGet(() -> inventoryRepository.save(Inventory.builder()
                        .product(product)
                        .warehouse(warehouse)
                        .quantityAvailable(quantity)
                        .quantityReserved(0)
                        .reorderPoint(reorderPoint)
                        .maxStockLevel(maxStock)
                        .build()));
    }
}
