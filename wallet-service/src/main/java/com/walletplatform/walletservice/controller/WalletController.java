package com.walletplatform.walletservice.controller;

import com.walletplatform.domain.EntryType;
import com.walletplatform.domain.Wallet;
import com.walletplatform.walletservice.dto.*;
import com.walletplatform.walletservice.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(
            @AuthenticationPrincipal UUID callerId,
            @Valid @RequestBody CreateWalletRequest request) {
        Wallet wallet = walletService.createWallet(callerId,request);

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
    @PostMapping("/deposit")
    public ResponseEntity<WalletResponse> deposit(
            @AuthenticationPrincipal UUID callerId,
            @Valid @RequestBody DepositRequest request) {
        Wallet wallet = walletService.deposit(callerId, request);

        WalletResponse response = WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .currency(wallet.getCurrency())
                .balance(wallet.getBalance())
                .status(wallet.getStatus())
                .createdAt(wallet.getCreatedAt())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<WalletResponse> withdraw(@AuthenticationPrincipal UUID callerId,
                                                   @Valid @RequestBody WithdrawRequest request) {
        Wallet wallet = walletService.withdraw(callerId, request);

        return ResponseEntity.ok(toResponse(walletService.withdraw(callerId, request)));
    }
    @PostMapping("/transfer")
    public ResponseEntity<WalletResponse> transfer(@AuthenticationPrincipal UUID callerId,
                                                   @Valid @RequestBody TransferRequest request) {
        Wallet wallet = walletService.transfer(callerId, request);

        return ResponseEntity.ok(toResponse(wallet));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WalletResponse> getWallet(@AuthenticationPrincipal UUID callerId,
                                                    @PathVariable("id") UUID id) {
        return ResponseEntity.ok(toResponse(walletService.getWallet(callerId, id)));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity< Page<LedgerEntryResponse>> getTransactions(
            @AuthenticationPrincipal UUID callerId,
            @PathVariable("id") UUID id,
            @RequestParam(name = "type", required = false) EntryType type,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(walletService.getTransactions(callerId,id,type,from, to, page, size));
    }

    @GetMapping("/{id}/statement")
    public ResponseEntity<StatementResponse> getStatement(
            @AuthenticationPrincipal UUID callerId,
            @PathVariable("id") UUID id,
            @RequestParam(name = "from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(walletService.getStatement(callerId,id, from, to));
    }

    private WalletResponse toResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .currency(wallet.getCurrency())
                .balance(wallet.getBalance())
                .status(wallet.getStatus())
                .createdAt(wallet.getCreatedAt())
                .build();
    }
}