package com.walletplatform.authservice.dto;

import com.walletplatform.authservice.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String email;
    private Role role;
    private LocalDateTime createdAt;
}