package com.walletplatform.walletservice.service;

import com.walletplatform.domain.EntryType;
import com.walletplatform.domain.LedgerEntry;
import com.walletplatform.domain.Transaction;
import com.walletplatform.domain.TransactionStatus;
import com.walletplatform.domain.TransactionType;
import com.walletplatform.domain.Wallet;
import com.walletplatform.domain.WalletStatus;
import com.walletplatform.repository.LedgerEntryRepository;
import com.walletplatform.repository.TransactionRepository;
import com.walletplatform.repository.WalletRepository;
import com.walletplatform.walletservice.dto.CreateWalletRequest;
import com.walletplatform.walletservice.dto.DepositRequest;
import com.walletplatform.walletservice.exception.IdempotencyKeyConflictException;
import com.walletplatform.walletservice.exception.WalletNotActiveException;
import com.walletplatform.walletservice.exception.WalletNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

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

    @Transactional
    public Wallet deposit(DepositRequest request) {

        // 1. Find the wallet by ID — throw if not found
        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new WalletNotFoundException(request.getWalletId()));

        // 1b. Idempotency: a retry with an already-used key returns the wallet instead of
        //     inserting a duplicate transaction (which would violate the UNIQUE constraint)
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            boolean sameDeposit = existing.get().getType() == TransactionType.DEPOSIT
                    && ledgerEntryRepository.findByTransaction(existing.get()).stream()
                    .anyMatch(entry -> entry.getWallet().getId().equals(wallet.getId())
                            && entry.getAmount().compareTo(request.getAmount()) == 0);
            if (!sameDeposit) {
                throw new IdempotencyKeyConflictException(request.getIdempotencyKey());
            }
            return wallet;
        }

        // 2. Check wallet status is ACTIVE — throw if not
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletNotActiveException(wallet.getStatus());
        }

        // 3. Calculate new balance
        BigDecimal newBalance = wallet.getBalance().add(request.getAmount());

        // 4. Build and save the Transaction
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey(request.getIdempotencyKey())
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        // 5. Build and save the LedgerEntry
        LedgerEntry ledgerEntry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .wallet(wallet)
                .transaction(transaction)
                .entryType(EntryType.CREDIT)
                .amount(request.getAmount())
                .balanceAfter(newBalance)
                .createdAt(LocalDateTime.now())
                .build();
        ledgerEntryRepository.save(ledgerEntry);

        // 6. Update wallet balance and save
        wallet.setBalance(newBalance);
        return walletRepository.save(wallet);
    }
}