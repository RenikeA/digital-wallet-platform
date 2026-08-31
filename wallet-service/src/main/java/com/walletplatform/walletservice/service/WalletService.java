package com.walletplatform.walletservice.service;

import com.walletplatform.domain.Wallet;
import com.walletplatform.domain.WalletStatus;
import com.walletplatform.repository.WalletRepository;
import com.walletplatform.walletservice.dto.CreateWalletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    public Wallet createWallet(CreateWalletRequest request) {
        Wallet wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .userId(request.getUserId())
                .currency(request.getCurrency())
                .balance(BigDecimal.ZERO)
                .status(WalletStatus.ACTIVE)
                .version(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return walletRepository.save(wallet);
    }
}