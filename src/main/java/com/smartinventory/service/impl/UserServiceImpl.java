package com.smartinventory.service.impl;

import com.smartinventory.dto.request.RegisterRequest;
import com.smartinventory.dto.response.UserResponse;
import com.smartinventory.entity.User;
import com.smartinventory.entity.Warehouse;
import com.smartinventory.enums.Role;
import com.smartinventory.exception.BusinessException;
import com.smartinventory.exception.DuplicateResourceException;
import com.smartinventory.exception.ResourceNotFoundException;
import com.smartinventory.repository.UserRepository;
import com.smartinventory.repository.WarehouseRepository;
import com.smartinventory.service.UserService;
import com.smartinventory.util.AppUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppUtils appUtils;

    @Override
    @Transactional
    public UserResponse createUser(RegisterRequest request, Role role) {
        if (userRepository.existsByUsername(request.getUsername()))
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' already exists");
        if (userRepository.existsByEmail(request.getEmail()))
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' already exists");

        User actor = appUtils.getCurrentUser();
        if (actor.getRole() == Role.ROLE_MANAGER && role != Role.ROLE_STAFF) {
            throw new BusinessException("Managers can create staff users only");
        }

        Warehouse assignedWarehouse = resolveAssignedWarehouse(request.getAssignedWarehouseId(), actor, role);

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .assignedWarehouse(assignedWarehouse)
                .role(role)
                .enabled(true)
                .accountNonLocked(true)
                .build();

        user = userRepository.save(user);
        log.info("Admin created new user '{}' with role {}", user.getUsername(), role);
        return AuthServiceImpl.mapToUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return AuthServiceImpl.mapToUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        return AuthServiceImpl.mapToUserResponse(appUtils.getCurrentUser());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Role role, String keyword, Pageable pageable) {
        return userRepository.searchUsers(role, keyword, pageable)
                .map(AuthServiceImpl::mapToUserResponse);
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Prevent disabling oneself
        if (user.getUsername().equals(appUtils.getCurrentUsername()) && !enabled) {
            throw new BusinessException("You cannot disable your own account");
        }

        user.setEnabled(enabled);
        user = userRepository.save(user);
        log.info("User '{}' status updated to enabled={}", user.getUsername(), enabled);
        return AuthServiceImpl.mapToUserResponse(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (user.getUsername().equals(appUtils.getCurrentUsername())) {
            throw new BusinessException("You cannot delete your own account");
        }

        User actor = appUtils.getCurrentUser();
        if (actor.getRole() == Role.ROLE_MANAGER) {
            if (user.getRole() != Role.ROLE_STAFF) {
                throw new BusinessException("Managers can delete staff users only");
            }
            if (actor.getAssignedWarehouse() != null
                    && (user.getAssignedWarehouse() == null
                    || !actor.getAssignedWarehouse().getId().equals(user.getAssignedWarehouse().getId()))) {
                throw new BusinessException("Managers can delete staff only from their assigned warehouse");
            }
        }

        userRepository.delete(user);
        log.info("User '{}' deleted", user.getUsername());
    }

    private Warehouse resolveAssignedWarehouse(Long requestedWarehouseId, User actor, Role role) {
        if (role != Role.ROLE_MANAGER && role != Role.ROLE_STAFF) {
            return null;
        }
        Long warehouseId = requestedWarehouseId;
        if (actor.getRole() == Role.ROLE_MANAGER && actor.getAssignedWarehouse() != null) {
            warehouseId = actor.getAssignedWarehouse().getId();
        }
        if (warehouseId == null) {
            return null;
        }
        Long finalWarehouseId = warehouseId;
        return warehouseRepository.findById(finalWarehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", finalWarehouseId));
    }
}
