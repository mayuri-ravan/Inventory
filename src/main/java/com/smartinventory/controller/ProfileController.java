package com.smartinventory.controller;

import com.smartinventory.dto.request.ChangePasswordRequest;
import com.smartinventory.dto.request.ProfileUpdateRequest;
import com.smartinventory.dto.response.ApiResponse;
import com.smartinventory.dto.response.UserResponse;
import com.smartinventory.service.ProfileService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
@Tag(name = "Profile")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;

    @PutMapping
    public ResponseEntity<ApiResponse<UserResponse>> update(@Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated", profileService.update(request)));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> password(@Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed"));
    }
}
