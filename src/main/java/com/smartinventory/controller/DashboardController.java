package com.smartinventory.controller;

import com.smartinventory.dto.response.ApiResponse;
import com.smartinventory.dto.response.DashboardResponse;
import com.smartinventory.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Role-aware real-time dashboard summary")
    public ResponseEntity<ApiResponse<DashboardResponse>> summary() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard summary fetched", dashboardService.currentDashboard()));
    }
}
