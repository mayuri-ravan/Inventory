package com.smartinventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private String role;
    @Builder.Default
    private Map<String, Object> totals = new LinkedHashMap<>();
    @Builder.Default
    private Map<String, BigDecimal> revenue = new LinkedHashMap<>();
    @Builder.Default
    private List<Map<String, Object>> lowStockProducts = new ArrayList<>();
    @Builder.Default
    private List<Map<String, Object>> recentActivities = new ArrayList<>();
    @Builder.Default
    private List<Map<String, Object>> warehousePerformance = new ArrayList<>();
    @Builder.Default
    private List<Map<String, Object>> topSellingProducts = new ArrayList<>();
    @Builder.Default
    private List<String> insights = new ArrayList<>();
    @Builder.Default
    private Map<String, Object> charts = new LinkedHashMap<>();
}
