package com.smartinventory.service;

import com.smartinventory.dto.response.DashboardResponse;
import com.smartinventory.entity.Inventory;
import com.smartinventory.entity.User;
import com.smartinventory.enums.OrderStatus;
import com.smartinventory.enums.ProductStatus;
import com.smartinventory.enums.PurchaseStatus;
import com.smartinventory.enums.Role;
import com.smartinventory.repository.*;
import com.smartinventory.util.AppUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final AuditLogService auditLogService;
    private final SmartInventoryService smartInventoryService;
    private final AppUtils appUtils;
    private final StockMovementRepository stockMovementRepository;

    @Transactional(readOnly = true)
    public DashboardResponse currentDashboard() {
        User user = appUtils.getCurrentUser();
        DashboardResponse response = DashboardResponse.builder()
                .role(user.getRole().name())
                .build();

        response.setTotals(buildTotals(user));
        response.setRevenue(buildRevenue());
        response.setLowStockProducts(lowStockProducts());
        response.setRecentActivities(auditLogService.recent());
        response.setWarehousePerformance(warehousePerformance());
        response.setTopSellingProducts(topProducts());
        response.setCharts(buildCharts());
        response.setInsights(smartInventoryService.businessInsights());

        if (user.getAssignedWarehouse() != null) {
            response.getTotals().put("assignedWarehouseId", user.getAssignedWarehouse().getId());
            response.getTotals().put("assignedWarehouseName", user.getAssignedWarehouse().getName());
            response.getTotals().put("assignedWarehouseLocation", user.getAssignedWarehouse().getLocation());
        }
        return response;
    }

    private Map<String, Object> buildTotals(User user) {
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("totalProducts", productRepository.count());
        totals.put("approvedProducts", productRepository.countByStatus(ProductStatus.APPROVED));
        totals.put("totalCategories", productRepository.findAllCategories().size());
        totals.put("totalWarehouses", warehouseRepository.count());
        totals.put("totalSuppliers", supplierRepository.count());
        totals.put("totalCustomers", userRepository.countByRole(Role.ROLE_CUSTOMER));
        totals.put("totalManagers", userRepository.countByRole(Role.ROLE_MANAGER));
        totals.put("totalStaff", userRepository.countByRole(Role.ROLE_STAFF));
        totals.put("totalSalesOrders", orderRepository.count());
        totals.put("totalPurchaseOrders", purchaseOrderRepository.count());
        totals.put("lowStockProducts", inventoryRepository.countLowStockItems());
        totals.put("pendingApprovals", purchaseOrderRepository.countByStatus(PurchaseStatus.PENDING_APPROVAL)
                + productRepository.countByStatus(ProductStatus.PENDING_APPROVAL));
        totals.put("pendingSalesOrders", orderRepository.countByStatus(OrderStatus.PENDING));
        totals.put("totalStockUnits", inventoryRepository.sumAvailableStock());

        if (user.getRole() == Role.ROLE_SUPPLIER) {
            totals.put("supplierWorkspace", true);
        }
        return totals;
    }

    private Map<String, BigDecimal> buildRevenue() {

        LocalDate today = LocalDate.now();

        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(LocalTime.MAX);

        YearMonth month = YearMonth.now();

        LocalDateTime monthStart = month.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = month.atEndOfMonth().atTime(LocalTime.MAX);

        BigDecimal orderRevenue =
                orderRepository.sumRevenue();

        BigDecimal manualStockRevenue =
                stockMovementRepository.getManualStockRevenue();

        BigDecimal returnRevenue =
                stockMovementRepository.getReturnedRevenue();

        BigDecimal totalRevenue =
                orderRevenue
                        .add(manualStockRevenue)
                        .subtract(returnRevenue);

        BigDecimal monthlyRevenue =
                orderRepository.sumRevenueBetween(monthStart, monthEnd)
                        .add(stockMovementRepository.getManualStockRevenueBetween(monthStart, monthEnd))
                        .subtract(stockMovementRepository.getReturnedRevenueBetween(monthStart, monthEnd));

        BigDecimal dailyRevenue =
                orderRepository.sumRevenueBetween(dayStart, dayEnd)
                        .add(stockMovementRepository.getManualStockRevenueBetween(dayStart, dayEnd))
                        .subtract(stockMovementRepository.getReturnedRevenueBetween(dayStart, dayEnd));

        Map<String, BigDecimal> revenue = new LinkedHashMap<>();

        revenue.put("totalRevenue", totalRevenue);
        revenue.put("monthlyRevenue", monthlyRevenue);
        revenue.put("dailyRevenue", dailyRevenue);
        revenue.put("monthlyPurchases",
                purchaseOrderRepository.sumPurchasesBetween(monthStart, monthEnd));

        return revenue;
    }
    private List<Map<String, Object>> lowStockProducts() {
        return inventoryRepository.findLowStockItems().stream()
                .limit(10)
                .map(this::inventoryMap)
                .toList();
    }

    private Map<String, Object> inventoryMap(Inventory inventory) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("productId", inventory.getProduct().getId());
        map.put("productName", inventory.getProduct().getName());
        map.put("productSku", inventory.getProduct().getSku());
        map.put("warehouseId", inventory.getWarehouse().getId());
        map.put("warehouseName", inventory.getWarehouse().getName());
        map.put("quantityAvailable", inventory.getQuantityAvailable());
        map.put("reorderPoint", inventory.getReorderPoint());
        map.put("severity", inventory.getQuantityAvailable() <= Math.max(1, inventory.getReorderPoint() / 2)
                ? "CRITICAL" : "LOW");
        return map;
    }

    private List<Map<String, Object>> warehousePerformance() {
        return inventoryRepository.warehousePerformance().stream().map(row -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("warehouseId", row[0]);
            map.put("warehouseName", row[1]);
            map.put("productCount", row[2]);
            map.put("availableUnits", row[3]);
            map.put("healthScore", smartInventoryService.inventoryHealthScore((Long) row[0]));
            return map;
        }).toList();
    }

    private List<Map<String, Object>> topProducts() {
        return orderItemRepository.topSelling(PageRequest.of(0, 10)).stream().map(row -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("productId", row[0]);
            map.put("productName", row[1]);
            map.put("sku", row[2]);
            map.put("unitsSold", row[3]);
            map.put("revenue", row[4]);
            return map;
        }).toList();
    }

    private Map<String, Object> buildCharts() {
        Map<String, Object> charts = new LinkedHashMap<>();
        Map<String, Long> orderStatus = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            orderStatus.put(status.name(), orderRepository.countByStatus(status));
        }
        charts.put("orderStatus", orderStatus);

        List<Map<String, Object>> stockByCategory = inventoryRepository.stockByCategory().stream().map(row -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("category", row[0] != null ? row[0] : "Uncategorized");
            map.put("available", row[1]);
            map.put("reserved", row[2]);
            return map;
        }).toList();
        charts.put("stockByCategory", stockByCategory);

        List<Map<String, Object>> monthly = new ArrayList<>();
        YearMonth cursor = YearMonth.now().minusMonths(11);
        for (int i = 0; i < 12; i++) {
            YearMonth ym = cursor.plusMonths(i);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("month", ym.toString());
            LocalDateTime start = ym.atDay(1).atStartOfDay();
            LocalDateTime end = ym.atEndOfMonth().atTime(LocalTime.MAX);

            BigDecimal revenue =
                    orderRepository.sumRevenueBetween(start, end)
                            .add(stockMovementRepository.getManualStockRevenueBetween(start, end))
                            .subtract(stockMovementRepository.getReturnedRevenueBetween(start, end));

            point.put("revenue", revenue);
            monthly.add(point);
        }
        charts.put("monthlyRevenue", monthly);
        return charts;
    }
}
