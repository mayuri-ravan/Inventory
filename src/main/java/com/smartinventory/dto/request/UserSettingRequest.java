package com.smartinventory.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserSettingRequest {
    @Size(max = 40)
    private String theme;

    @Size(max = 20)
    private String language;

    private Boolean emailNotifications;
    private Boolean toastNotifications;
    private Boolean lowStockAlerts;

    @Size(max = 40)
    private String dashboardLayout;
}
