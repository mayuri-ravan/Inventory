package com.smartinventory.entity;

import com.smartinventory.enums.NotificationPriority;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Notification-center item for user and role-scoped events. */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_recipient", columnList = "recipient_id"),
        @Index(name = "idx_notification_role", columnList = "recipient_role"),
        @Index(name = "idx_notification_read", columnList = "read_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @Column(name = "recipient_role", length = 30)
    private String recipientRole;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 700)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.NORMAL;

    @Column(length = 80)
    private String eventType;

    @Column(length = 80)
    private String entityType;

    @Column
    private Long entityId;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public boolean isRead() {
        return readAt != null;
    }
}
