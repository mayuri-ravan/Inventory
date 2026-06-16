package com.smartinventory.service;

import com.smartinventory.entity.AuditLog;
import com.smartinventory.entity.User;
import com.smartinventory.repository.AuditLogRepository;
import com.smartinventory.util.AppUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AppUtils appUtils;

    @Transactional
    public void record(String action, String entityType, Long entityId, String details) {
        User user = null;
        try {
            user = appUtils.getCurrentUser();
        } catch (RuntimeException ignored) {
            // Public flows such as forgot-password are still auditable without a principal.
        }

        auditLogRepository.save(AuditLog.builder()
                .user(user)
                .username(user != null ? user.getUsername() : "anonymous")
                .userRole(user != null ? user.getRole().name() : "PUBLIC")
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .ipAddress("local")
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> search(String action, String username, LocalDateTime from, LocalDateTime to,
                                            Pageable pageable) {
        return auditLogRepository.search(action, username, from, to, pageable).map(this::toMap);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> recent() {
        return auditLogRepository.findTop10ByOrderByCreatedAtDesc().stream().map(this::toMap).toList();
    }

    private Map<String, Object> toMap(AuditLog auditLog) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", auditLog.getId());
        map.put("username", auditLog.getUsername());
        map.put("userRole", auditLog.getUserRole());
        map.put("action", auditLog.getAction());
        map.put("entityType", auditLog.getEntityType());
        map.put("entityId", auditLog.getEntityId());
        map.put("details", auditLog.getDetails());
        map.put("ipAddress", auditLog.getIpAddress());
        map.put("createdAt", auditLog.getCreatedAt());
        return map;
    }
}
