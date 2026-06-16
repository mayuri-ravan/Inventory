package com.smartinventory.service;

import com.smartinventory.dto.request.UserSettingRequest;
import com.smartinventory.dto.response.UserSettingResponse;
import com.smartinventory.entity.User;
import com.smartinventory.entity.UserSetting;
import com.smartinventory.repository.UserSettingRepository;
import com.smartinventory.util.AppUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSettingService {

    private final UserSettingRepository userSettingRepository;
    private final AppUtils appUtils;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public UserSettingResponse getCurrent() {
        return toResponse(getOrCreate(appUtils.getCurrentUser()));
    }

    @Transactional
    public UserSettingResponse update(UserSettingRequest request) {
        User user = appUtils.getCurrentUser();
        UserSetting setting = getOrCreate(user);
        if (request.getTheme() != null) setting.setTheme(request.getTheme());
        if (request.getLanguage() != null) setting.setLanguage(request.getLanguage());
        if (request.getEmailNotifications() != null) setting.setEmailNotifications(request.getEmailNotifications());
        if (request.getToastNotifications() != null) setting.setToastNotifications(request.getToastNotifications());
        if (request.getLowStockAlerts() != null) setting.setLowStockAlerts(request.getLowStockAlerts());
        if (request.getDashboardLayout() != null) setting.setDashboardLayout(request.getDashboardLayout());
        UserSetting saved = userSettingRepository.save(setting);
        auditLogService.record("USER_SETTINGS_UPDATED", "UserSetting", saved.getId(), "Updated account preferences");
        return toResponse(saved);
    }

    private UserSetting getOrCreate(User user) {
        return userSettingRepository.findByUserId(user.getId())
                .orElseGet(() -> userSettingRepository.save(UserSetting.builder().user(user).build()));
    }

    private UserSettingResponse toResponse(UserSetting setting) {
        return UserSettingResponse.builder()
                .theme(setting.getTheme())
                .language(setting.getLanguage())
                .emailNotifications(setting.isEmailNotifications())
                .toastNotifications(setting.isToastNotifications())
                .lowStockAlerts(setting.isLowStockAlerts())
                .dashboardLayout(setting.getDashboardLayout())
                .build();
    }
}
