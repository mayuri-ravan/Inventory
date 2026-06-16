package com.smartinventory.service;

import com.smartinventory.entity.Inventory;
import com.smartinventory.repository.InventoryRepository;
import com.smartinventory.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SmartInventoryService {

    private final InventoryRepository inventoryRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> reorderRecommendations() {
        return inventoryRepository.findLowStockItems().stream().map(inventory -> {
            int reorderPoint = Math.max(1, inventory.getReorderPoint());
            int deficit = Math.max(0, reorderPoint - inventory.getQuantityAvailable());
            int suggestedQty = Math.max(reorderPoint * 2, deficit + reorderPoint);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("productId", inventory.getProduct().getId());
            map.put("productName", inventory.getProduct().getName());
            map.put("warehouseId", inventory.getWarehouse().getId());
            map.put("warehouseName", inventory.getWarehouse().getName());
            map.put("currentStock", inventory.getQuantityAvailable());
            map.put("reorderPoint", reorderPoint);
            map.put("suggestedQuantity", suggestedQty);
            map.put("reason", inventory.getQuantityAvailable() <= reorderPoint / 2
                    ? "Critical stock level" : "Below reorder point");
            return map;
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> demandForecast() {
        return orderItemRepository.topSelling(PageRequest.of(0, 8)).stream().map(row -> {
            long unitsSold = ((Number) row[3]).longValue();
            BigDecimal weeklyDemand = BigDecimal.valueOf(unitsSold)
                    .divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("productId", row[0]);
            map.put("productName", row[1]);
            map.put("sku", row[2]);
            map.put("lastPeriodUnits", unitsSold);
            map.put("forecastNextWeek", weeklyDemand);
            map.put("trend", unitsSold > 20 ? "RISING" : unitsSold > 5 ? "STABLE" : "LOW");
            return map;
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> slowMovingProducts() {
        return orderItemRepository.slowMoving(PageRequest.of(0, 8)).stream().map(row -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("productId", row[0]);
            map.put("productName", row[1]);
            map.put("sku", row[2]);
            map.put("unitsSold", row[3]);
            map.put("revenue", row[4]);
            return map;
        }).toList();
    }

    @Transactional(readOnly = true)
    public int inventoryHealthScore(Long warehouseId) {
        List<Inventory> inventory = warehouseId == null
                ? inventoryRepository.findAll()
                : inventoryRepository.findByWarehouseId(warehouseId);
        if (inventory.isEmpty()) return 100;
        long lowStock = inventory.stream().filter(i -> i.getQuantityAvailable() <= i.getReorderPoint()).count();
        long overstock = inventory.stream()
                .filter(i -> i.getMaxStockLevel() != null && i.getQuantityAvailable() > i.getMaxStockLevel())
                .count();
        int penalty = (int) Math.min(70, ((lowStock + overstock) * 100) / inventory.size());
        return Math.max(30, 100 - penalty);
    }

    @Transactional(readOnly = true)
    public List<String> businessInsights() {
        long lowStock = inventoryRepository.countLowStockItems();
        long totalStock = inventoryRepository.sumAvailableStock();
        List<Map<String, Object>> reorder = reorderRecommendations();
        return List.of(
                lowStock > 0
                        ? lowStock + " products need attention before they interrupt order fulfillment."
                        : "Inventory is healthy across all tracked reorder points.",
                totalStock < 100
                        ? "Total available stock is lean; review purchase planning for fast-moving items."
                        : "Available stock coverage is stable for current demand.",
                reorder.isEmpty()
                        ? "No automatic reorder recommendation is pending."
                        : "Smart reorder generated " + reorder.size() + " purchase suggestions."
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> analyticsSummary() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("reorderRecommendations", reorderRecommendations());
        map.put("demandForecast", demandForecast());
        map.put("slowMovingProducts", slowMovingProducts());
        map.put("inventoryHealthScore", inventoryHealthScore(null));
        map.put("insights", businessInsights());
        return map;
    }
}
