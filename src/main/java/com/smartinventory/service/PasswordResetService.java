package com.smartinventory.service;

import com.smartinventory.dto.request.ForgotPasswordRequest;
import com.smartinventory.dto.request.ResetPasswordRequest;
import com.smartinventory.dto.request.VerifyOtpRequest;
import com.smartinventory.entity.PasswordResetOtp;
import com.smartinventory.entity.User;
import com.smartinventory.exception.BusinessException;
import com.smartinventory.exception.ResourceNotFoundException;
import com.smartinventory.repository.PasswordResetOtpRepository;
import com.smartinventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional
    public Map<String, Object> start(ForgotPasswordRequest request) {
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            passwordResetOtpRepository.save(PasswordResetOtp.builder()
                    .email(request.getEmail())
                    .otp(otp)
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .build());
            auditLogService.record("PASSWORD_RESET_REQUESTED", "User", null,
                    "Password reset requested for " + request.getEmail());
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "If the account exists, an OTP has been generated.");
        response.put("expiresInMinutes", 10);
        response.put("demoOtp", otp);
        return response;
    }

    @Transactional
    public Map<String, Object> verify(VerifyOtpRequest request) {
        PasswordResetOtp otp = getValidOtp(request.getEmail(), request.getOtp());
        otp.setVerifiedAt(LocalDateTime.now());
        passwordResetOtpRepository.save(otp);
        return Map.of("verified", true, "message", "OTP verified");
    }

    @Transactional
    public void reset(ResetPasswordRequest request) {
        PasswordResetOtp otp = getValidOtp(request.getEmail(), request.getOtp());
        if (otp.getVerifiedAt() == null) {
            throw new BusinessException("Verify OTP before resetting password");
        }
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        otp.setUsedAt(LocalDateTime.now());
        passwordResetOtpRepository.save(otp);
        auditLogService.record("PASSWORD_RESET_COMPLETED", "User", user.getId(), "Password reset completed");
    }

    private PasswordResetOtp getValidOtp(String email, String otpValue) {
        PasswordResetOtp otp = passwordResetOtpRepository.findTopByEmailAndOtpOrderByCreatedAtDesc(email, otpValue)
                .orElseThrow(() -> new BusinessException("Invalid OTP"));
        if (otp.isExpired()) throw new BusinessException("OTP has expired");
        if (otp.getUsedAt() != null) throw new BusinessException("OTP has already been used");
        return otp;
    }
}
