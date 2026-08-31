package com.walletplatform.walletservice.dto;

import com.walletplatform.domain.WalletStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class WalletResponse {
    private UUID id;
    private UUID userId;
    private String currency;
    private BigDecimal balance;
    private WalletStatus status;
    private LocalDateTime createdAt;
}