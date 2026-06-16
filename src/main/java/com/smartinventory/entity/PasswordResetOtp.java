package com.smartinventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Short-lived OTP used by the forgot-password flow. */
@Entity
@Table(name = "password_reset_otps", indexes = {
        @Index(name = "idx_reset_email", columnList = "email"),
        @Index(name = "idx_reset_otp", columnList = "otp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(nullable = false, length = 8)
    private String otp;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
