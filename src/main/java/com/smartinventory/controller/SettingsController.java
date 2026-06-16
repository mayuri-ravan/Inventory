package com.smartinventory.controller;

import com.smartinventory.dto.request.UserSettingRequest;
import com.smartinventory.dto.response.ApiResponse;
import com.smartinventory.dto.response.UserSettingResponse;
import com.smartinventory.service.UserSettingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
@Tag(name = "User Settings")
@SecurityRequirement(name = "bearerAuth")
public class SettingsController {

    private final UserSettingService userSettingService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserSettingResponse>> get() {
        return ResponseEntity.ok(ApiResponse.success("Settings fetched", userSettingService.getCurrent()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserSettingResponse>> update(@Valid @RequestBody UserSettingRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Settings updated", userSettingService.update(request)));
    }
}
