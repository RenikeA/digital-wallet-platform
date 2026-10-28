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
import com.walletplatform.walletservice.dto.*;
import com.walletplatform.walletservice.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
    @Transactional
    public Wallet withdraw(WithdrawRequest request) {

        // 1. Find the wallet by ID, throw if not found
        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new WalletNotFoundException(request.getWalletId()));

        // 2. Idempotency: a retry with an already-used key returns the wallet
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            boolean sameWithdrawal = existing.get().getType() == TransactionType.WITHDRAWAL
                    && ledgerEntryRepository.findByTransaction(existing.get()).stream()
                    .anyMatch(entry -> entry.getWallet().getId().equals(wallet.getId())
                            && entry.getAmount().compareTo(request.getAmount()) == 0);
            if (!sameWithdrawal) {
                throw new IdempotencyKeyConflictException(request.getIdempotencyKey());
            }
            return wallet;
        }

        // 3. Check the wallet is ACTIVE
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletNotActiveException(wallet.getStatus());
        }

        // 4. Check there are enough funds
        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(wallet.getBalance(), request.getAmount());
        }

        // 5. Work out the new balance, subtract this time
        BigDecimal newBalance = wallet.getBalance().subtract(request.getAmount());

        // 6. Save the Transaction with type WITHDRAWAL
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey(request.getIdempotencyKey())
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        // 7. Save the LedgerEntry with type DEBIT
        LedgerEntry ledgerEntry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .wallet(wallet)
                .transaction(transaction)
                .entryType(EntryType.DEBIT)
                .amount(request.getAmount())
                .balanceAfter(newBalance)
                .createdAt(LocalDateTime.now())
                .build();
        ledgerEntryRepository.save(ledgerEntry);

        // 8. Update the wallet balance and save
        wallet.setBalance(newBalance);
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet transfer(TransferRequest request) {

        // 1. Source and destination must be different wallets
        if (request.getSourceWalletId().equals(request.getDestinationWalletId())) {
            throw new SameWalletTransferException();
        }

        // 2. Find both wallets
        Wallet sourceWallet = walletRepository.findById(request.getSourceWalletId())
                .orElseThrow(() -> new WalletNotFoundException(request.getSourceWalletId()));
        Wallet destinationWallet = walletRepository.findById(request.getDestinationWalletId())
                .orElseThrow(() -> new WalletNotFoundException(request.getDestinationWalletId()));

        // 3. Idempotency: the same key with the same transfer returns the source wallet
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            List<LedgerEntry> entries = ledgerEntryRepository.findByTransaction(existing.get());

            boolean debitMatches = entries.stream().anyMatch(entry ->
                    entry.getEntryType() == EntryType.DEBIT
                            && entry.getWallet().getId().equals(sourceWallet.getId())
                            && entry.getAmount().compareTo(request.getAmount()) == 0);

            boolean creditMatches = entries.stream().anyMatch(entry ->
                    entry.getEntryType() == EntryType.CREDIT
                            && entry.getWallet().getId().equals(destinationWallet.getId())
                            && entry.getAmount().compareTo(request.getAmount()) == 0);

            boolean sameTransfer = existing.get().getType() == TransactionType.TRANSFER
                    && debitMatches && creditMatches;

            if (!sameTransfer) {
                throw new IdempotencyKeyConflictException(request.getIdempotencyKey());
            }
            return sourceWallet;
        }

        // 4. Both wallets must be ACTIVE
        if (sourceWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletNotActiveException(sourceWallet.getStatus());
        }
        if (destinationWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletNotActiveException(destinationWallet.getStatus());
        }

        // 5. Both wallets must use the same currency
        if (!sourceWallet.getCurrency().equals(destinationWallet.getCurrency())) {
            throw new CurrencyMismatchException(sourceWallet.getCurrency(), destinationWallet.getCurrency());
        }

        // 6. The source wallet must have enough money
        if (sourceWallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(sourceWallet.getBalance(), request.getAmount());
        }

        // 7. Work out both new balances
        BigDecimal sourceNewBalance = sourceWallet.getBalance().subtract(request.getAmount());
        BigDecimal destinationNewBalance = destinationWallet.getBalance().add(request.getAmount());

        // 8. Save ONE transaction and TWO ledger entries
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey(request.getIdempotencyKey())
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        LedgerEntry debitEntry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .wallet(sourceWallet)
                .transaction(transaction)
                .entryType(EntryType.DEBIT)
                .amount(request.getAmount())
                .balanceAfter(sourceNewBalance)
                .createdAt(LocalDateTime.now())
                .build();

        LedgerEntry creditEntry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .wallet(destinationWallet)
                .transaction(transaction)
                .entryType(EntryType.CREDIT)
                .amount(request.getAmount())
                .balanceAfter(destinationNewBalance)
                .createdAt(LocalDateTime.now())
                .build();

        ledgerEntryRepository.save(debitEntry);
        ledgerEntryRepository.save(creditEntry);

        // 9. Update both wallet balances
        sourceWallet.setBalance(sourceNewBalance);
        destinationWallet.setBalance(destinationNewBalance);
        walletRepository.save(sourceWallet);
        walletRepository.save(destinationWallet);

        return sourceWallet;
    }

    @Transactional(readOnly = true)
    public Wallet getWallet(UUID id) {
        return walletRepository.findById(id)
                .orElseThrow(() -> new WalletNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntryResponse> getTransactions(UUID walletId, EntryType type, LocalDate from, LocalDate to, int page, int size) {
        walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));

        int safeSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by("createdAt").descending());
        Page<LedgerEntry> entries;
        if (from != null && to != null) {
            LocalDateTime start = from.atStartOfDay();
            LocalDateTime end = to.plusDays(1).atStartOfDay();
            entries = ledgerEntryRepository.findByWalletIdAndCreatedAtBetween(walletId, start, end, pageable);
        } else if (type != null) {
            entries = ledgerEntryRepository.findByWalletIdAndEntryType(walletId, type, pageable);
        } else {
            entries = ledgerEntryRepository.findByWalletId(walletId, pageable);
        }

        return entries
                .map(entry -> new LedgerEntryResponse(
                        entry.getId(),
                        entry.getTransaction().getId(),
                        entry.getEntryType(),
                        entry.getAmount(),
                        entry.getBalanceAfter(),
                        entry.getCreatedAt()));
    }
}