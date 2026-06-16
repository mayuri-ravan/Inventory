package com.smartinventory.service.impl;

import com.smartinventory.dto.request.LoginRequest;
import com.smartinventory.dto.request.RegisterRequest;
import com.smartinventory.dto.response.AuthResponse;
import com.smartinventory.dto.response.UserResponse;
import com.smartinventory.entity.User;
import com.smartinventory.enums.Role;
import com.smartinventory.exception.BusinessException;
import com.smartinventory.exception.DuplicateResourceException;
import com.smartinventory.exception.ResourceNotFoundException;
import com.smartinventory.repository.UserRepository;
import com.smartinventory.service.AuditLogService;
import com.smartinventory.security.JwtUtils;
import com.smartinventory.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for: {}", request.getUsernameOrEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtUtils.generateAccessToken(authentication);
        String refreshToken = jwtUtils.generateRefreshToken(authentication.getName());

        User user = userRepository.findByUsername(authentication.getName())
                .or(() -> userRepository.findByEmail(authentication.getName()))
                .orElseThrow();

        log.info("User '{}' logged in with role {}", user.getUsername(), user.getRole());
        auditLogService.record("LOGIN", "User", user.getId(), "User logged in");

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .expiresIn(jwtUtils.getExpirationMs())
                .build();
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername()))
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        if (userRepository.existsByEmail(request.getEmail()))
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(Role.ROLE_CUSTOMER)
                .enabled(true)
                .accountNonLocked(true)
                .build();

        user = userRepository.save(user);
        log.info("New customer registered: {}", user.getUsername());
        auditLogService.record("USER_REGISTERED", "User", user.getId(), "Customer self-registration");
        return mapToUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtils.validateToken(refreshToken))
            throw new BusinessException("Invalid or expired refresh token");

        String username = jwtUtils.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        return AuthResponse.builder()
                .accessToken(jwtUtils.generateAccessToken(username))
                .refreshToken(jwtUtils.generateRefreshToken(username))
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .expiresIn(jwtUtils.getExpirationMs())
                .build();
    }

    public static UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .profileImageUrl(user.getProfileImageUrl())
                .assignedWarehouseId(user.getAssignedWarehouse() != null ? user.getAssignedWarehouse().getId() : null)
                .assignedWarehouseName(user.getAssignedWarehouse() != null ? user.getAssignedWarehouse().getName() : null)
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
