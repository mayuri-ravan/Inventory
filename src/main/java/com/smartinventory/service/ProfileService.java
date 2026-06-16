package com.smartinventory.service;

import com.smartinventory.dto.request.ChangePasswordRequest;
import com.smartinventory.dto.request.ProfileUpdateRequest;
import com.smartinventory.dto.response.UserResponse;
import com.smartinventory.entity.User;
import com.smartinventory.exception.BusinessException;
import com.smartinventory.exception.DuplicateResourceException;
import com.smartinventory.repository.UserRepository;
import com.smartinventory.service.impl.AuthServiceImpl;
import com.smartinventory.util.AppUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppUtils appUtils;
    private final AuditLogService auditLogService;

    @Transactional
    public UserResponse update(ProfileUpdateRequest request) {
        User user = appUtils.getCurrentUser();
        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");
        }
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getProfileImageUrl() != null) user.setProfileImageUrl(request.getProfileImageUrl());
        User saved = userRepository.save(user);
        auditLogService.record("PROFILE_UPDATED", "User", saved.getId(), "Profile details updated");
        return AuthServiceImpl.mapToUserResponse(saved);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = appUtils.getCurrentUser();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        auditLogService.record("PASSWORD_CHANGED", "User", user.getId(), "Password changed from profile page");
    }
}
