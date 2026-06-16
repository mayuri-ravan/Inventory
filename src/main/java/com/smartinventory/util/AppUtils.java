package com.smartinventory.util;

import com.smartinventory.entity.User;
import com.smartinventory.exception.ResourceNotFoundException;
import com.smartinventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility component providing helper methods for security context access
 * and unique identifier generation.
 */
@Component
@RequiredArgsConstructor
public class AppUtils {

    private final UserRepository userRepository;
    private static final AtomicLong SEQUENCE = new AtomicLong(0);
    private static final DateTimeFormatter ORDER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Returns the currently authenticated username from the security context.
     */
    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user in security context");
        }
        return auth.getName();
    }

    /**
     * Returns the currently authenticated User entity.
     */
    public User getCurrentUser() {
        String username = getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    /**
     * Generates a unique order number in format: ORD-20240101120000-0001
     */
    public String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(ORDER_DATE_FORMAT);
        long seq = SEQUENCE.incrementAndGet() % 10000;
        return String.format("ORD-%s-%04d", timestamp, seq);
    }

    /**
     * Generates a unique SKU suffix for products.
     */
    public static String generateSkuSuffix() {
        return String.valueOf(System.currentTimeMillis()).substring(7);
    }
}
