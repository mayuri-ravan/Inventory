package com.smartinventory.entity;

import jakarta.persistence.*;
import lombok.*;

/** Per-user preferences for theme, notification and dashboard behavior. */
@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String theme = "light";

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String language = "en";

    @Column(name = "email_notifications", nullable = false)
    @Builder.Default
    private boolean emailNotifications = true;

    @Column(name = "toast_notifications", nullable = false)
    @Builder.Default
    private boolean toastNotifications = true;

    @Column(name = "low_stock_alerts", nullable = false)
    @Builder.Default
    private boolean lowStockAlerts = true;

    @Column(name = "dashboard_layout", length = 40)
    @Builder.Default
    private String dashboardLayout = "role";
}
