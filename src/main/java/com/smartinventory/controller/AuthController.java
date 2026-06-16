package com.smartinventory.controller;

import com.smartinventory.dto.request.ForgotPasswordRequest;
import com.smartinventory.dto.request.LoginRequest;
import com.smartinventory.dto.request.RegisterRequest;
import com.smartinventory.dto.request.ResetPasswordRequest;
import com.smartinventory.dto.request.VerifyOtpRequest;
import com.smartinventory.dto.response.ApiResponse;
import com.smartinventory.dto.response.AuthResponse;
import com.smartinventory.dto.response.UserResponse;
import com.smartinventory.service.AuthService;
import com.smartinventory.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Login, registration, and token management")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    @Operation(summary = "User login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request)));
    }

    @PostMapping("/register")
    @Operation(summary = "Customer self-registration")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", authService.register(request)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestParam String refreshToken) {
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", authService.refreshToken(refreshToken)));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Start forgot password OTP flow")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Password reset OTP generated",
                passwordResetService.start(request)));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify forgot password OTP")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.success("OTP verified", passwordResetService.verify(request)));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password after OTP verification")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful"));
    }
}
