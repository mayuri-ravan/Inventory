package com.smartinventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Stores business and security activities for compliance reporting. */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_user", columnList = "user_id"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "username", length = 80)
    private String username;

    @Column(name = "user_role", length = 30)
    private String userRole;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(length = 80)
    private String entityType;

    @Column
    private Long entityId;

    @Column(length = 500)
    private String details;

    @Column(name = "ip_address", length = 80)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
