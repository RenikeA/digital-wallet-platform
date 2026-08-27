package com.walletplatform.authservice.controller;

import com.walletplatform.authservice.domain.User;
import com.walletplatform.authservice.dto.AuthResponse;
import com.walletplatform.authservice.dto.LoginRequest;
import com.walletplatform.authservice.dto.RegisterRequest;
import com.walletplatform.authservice.dto.UserResponse;
import com.walletplatform.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User createdUser = authService.register(request);

        UserResponse response = UserResponse.builder()
                .id(createdUser.getId())
                .email(createdUser.getEmail())
                .role(createdUser.getRole())
                .createdAt(createdUser.getCreatedAt())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}