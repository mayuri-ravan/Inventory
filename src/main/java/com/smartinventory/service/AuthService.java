package com.smartinventory.service;

import com.smartinventory.dto.request.LoginRequest;
import com.smartinventory.dto.request.RegisterRequest;
import com.smartinventory.dto.response.AuthResponse;
import com.smartinventory.dto.response.UserResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    UserResponse register(RegisterRequest request);
    AuthResponse refreshToken(String refreshToken);
}
