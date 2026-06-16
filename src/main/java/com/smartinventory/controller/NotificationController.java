package com.smartinventory.controller;

import com.smartinventory.dto.response.ApiResponse;
import com.smartinventory.dto.response.NotificationResponse;
import com.smartinventory.service.NotificationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched",
                notificationService.getMyNotifications(PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> unread() {
        return ResponseEntity.ok(ApiResponse.success("Unread count fetched", notificationService.unreadCount()));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> read(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Notification marked read", notificationService.markRead(id)));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> readAll() {
        notificationService.markAllRead();
        return ResponseEntity.ok(ApiResponse.success("All notifications marked read"));
    }
}
