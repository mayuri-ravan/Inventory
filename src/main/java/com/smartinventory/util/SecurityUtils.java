package com.smartinventory.util;

import com.smartinventory.entity.User;
import com.smartinventory.exception.ResourceNotFoundException;
import com.smartinventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility class for retrieving the currently authenticated user.
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    /** Returns the username of the currently authenticated user. */
    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }
        return auth.getName();
    }

    /** Returns the full User entity for the currently authenticated user. */
    public User getCurrentUser() {
        String username = getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    /** Returns the ID of the currently authenticated user. */
    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /** Checks whether the current user has the given role string (e.g., "ROLE_ADMIN"). */
    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }
}
