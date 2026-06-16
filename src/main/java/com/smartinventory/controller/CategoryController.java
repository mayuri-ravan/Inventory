package com.smartinventory.controller;

import com.smartinventory.dto.response.ApiResponse;
import com.smartinventory.service.CategoryIntelligenceService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Category Intelligence")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryIntelligenceService categoryIntelligenceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<String>>> search(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success("Categories fetched",
                categoryIntelligenceService.search(keyword)));
    }

    @GetMapping("/suggest")
    public ResponseEntity<ApiResponse<Map<String, Object>>> suggest(@RequestParam String productName) {
        return ResponseEntity.ok(ApiResponse.success("Category suggestion generated",
                categoryIntelligenceService.suggestions(productName)));
    }
}
