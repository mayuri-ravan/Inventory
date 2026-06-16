package com.smartinventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingResponse {
    private String theme;
    private String language;
    private boolean emailNotifications;
    private boolean toastNotifications;
    private boolean lowStockAlerts;
    private String dashboardLayout;
}
