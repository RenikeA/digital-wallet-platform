package com.walletplatform.walletservice.controller;

import com.walletplatform.domain.Wallet;
import com.walletplatform.walletservice.dto.CreateWalletRequest;
import com.walletplatform.walletservice.dto.WalletResponse;
import com.walletplatform.walletservice.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        Wallet wallet = walletService.createWallet(request);

        WalletResponse response = WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .currency(wallet.getCurrency())
                .balance(wallet.getBalance())
                .status(wallet.getStatus())
                .createdAt(wallet.getCreatedAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}