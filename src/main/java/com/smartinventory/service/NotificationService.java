package com.smartinventory.service;

import com.smartinventory.dto.response.NotificationResponse;
import com.smartinventory.entity.Notification;
import com.smartinventory.entity.User;
import com.smartinventory.enums.NotificationPriority;
import com.smartinventory.enums.Role;
import com.smartinventory.exception.ResourceNotFoundException;
import com.smartinventory.repository.NotificationRepository;
import com.smartinventory.util.AppUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AppUtils appUtils;

    @Transactional
    public void notifyUser(User user, String title, String message, NotificationPriority priority,
                           String eventType, String entityType, Long entityId) {
        notificationRepository.save(Notification.builder()
                .recipient(user)
                .title(title)
                .message(message)
                .priority(priority)
                .eventType(eventType)
                .entityType(entityType)
                .entityId(entityId)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public void notifyRole(Role role, String title, String message, NotificationPriority priority,
                           String eventType, String entityType, Long entityId) {
        notificationRepository.save(Notification.builder()
                .recipientRole(role.name())
                .title(title)
                .message(message)
                .priority(priority)
                .eventType(eventType)
                .entityType(entityType)
                .entityId(entityId)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(Pageable pageable) {
        User user = appUtils.getCurrentUser();
        return notificationRepository.findVisible(user.getId(), user.getRole().name(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        User user = appUtils.getCurrentUser();
        return notificationRepository.countUnread(user.getId(), user.getRole().name());
    }

    @Transactional
    public NotificationResponse markRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
        notification.setReadAt(LocalDateTime.now());
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllRead() {
        getMyNotifications(Pageable.unpaged()).forEach(n -> {
            Notification notification = notificationRepository.findById(n.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", n.getId()));
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        });
    }

    public NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .priority(notification.getPriority())
                .eventType(notification.getEventType())
                .entityType(notification.getEntityType())
                .entityId(notification.getEntityId())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
