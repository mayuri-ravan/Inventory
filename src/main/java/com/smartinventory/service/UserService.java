package com.smartinventory.service;

import com.smartinventory.dto.request.RegisterRequest;
import com.smartinventory.dto.response.UserResponse;
import com.smartinventory.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponse createUser(RegisterRequest request, Role role);
    UserResponse getUserById(Long id);
    UserResponse getCurrentUserProfile();
    Page<UserResponse> getAllUsers(Role role, String keyword, Pageable pageable);
    UserResponse updateUserStatus(Long id, boolean enabled);
    void deleteUser(Long id);
}
