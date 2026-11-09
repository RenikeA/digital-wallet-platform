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
import com.walletplatform.walletservice.specifications.LedgerEntrySpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    public Wallet createWallet(UUID callerId, CreateWalletRequest request) {
        Wallet wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .userId(callerId)
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
    public Wallet deposit(UUID callerId, DepositRequest request) {
        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new WalletNotFoundException(request.getWalletId()));
        checkOwner(wallet, callerId);

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

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletNotActiveException(wallet.getStatus());
        }

        BigDecimal newBalance = wallet.getBalance().add(request.getAmount());

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey(request.getIdempotencyKey())
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

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

        wallet.setBalance(newBalance);
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet withdraw(UUID callerId, WithdrawRequest request) {
        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new WalletNotFoundException(request.getWalletId()));
        checkOwner(wallet, callerId);

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

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletNotActiveException(wallet.getStatus());
        }

        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(wallet.getBalance(), request.getAmount());
        }

        BigDecimal newBalance = wallet.getBalance().subtract(request.getAmount());

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey(request.getIdempotencyKey())
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

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

        wallet.setBalance(newBalance);
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet transfer(UUID callerId, TransferRequest request) {
        if (request.getSourceWalletId().equals(request.getDestinationWalletId())) {
            throw new SameWalletTransferException();
        }

        Wallet sourceWallet = walletRepository.findById(request.getSourceWalletId())
                .orElseThrow(() -> new WalletNotFoundException(request.getSourceWalletId()));
        Wallet destinationWallet = walletRepository.findById(request.getDestinationWalletId())
                .orElseThrow(() -> new WalletNotFoundException(request.getDestinationWalletId()));

        // only the sender's wallet must belong to the caller
        checkOwner(sourceWallet, callerId);

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

        if (sourceWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletNotActiveException(sourceWallet.getStatus());
        }
        if (destinationWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletNotActiveException(destinationWallet.getStatus());
        }

        if (!sourceWallet.getCurrency().equals(destinationWallet.getCurrency())) {
            throw new CurrencyMismatchException(sourceWallet.getCurrency(), destinationWallet.getCurrency());
        }

        if (sourceWallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(sourceWallet.getBalance(), request.getAmount());
        }

        BigDecimal sourceNewBalance = sourceWallet.getBalance().subtract(request.getAmount());
        BigDecimal destinationNewBalance = destinationWallet.getBalance().add(request.getAmount());

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

        sourceWallet.setBalance(sourceNewBalance);
        destinationWallet.setBalance(destinationNewBalance);
        walletRepository.save(sourceWallet);
        walletRepository.save(destinationWallet);

        return sourceWallet;
    }

    @Transactional(readOnly = true)
    public Wallet getWallet(UUID callerId, UUID id) {
        Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new WalletNotFoundException(id));
        checkOwner(wallet, callerId);
        return wallet;
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntryResponse> getTransactions(UUID callerId, UUID walletId, EntryType type,
                                                     LocalDate from, LocalDate to, int page, int size) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
        checkOwner(wallet, callerId);

        int safeSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by("createdAt").descending());

        Specification<LedgerEntry> spec = LedgerEntrySpecifications.forWallet(walletId);
        if (type != null) {
            spec = spec.and(LedgerEntrySpecifications.hasType(type));
        }
        if (from != null) {
            spec = spec.and(LedgerEntrySpecifications.createdFrom(from.atStartOfDay()));
        }
        if (to != null) {
            spec = spec.and(LedgerEntrySpecifications.createdBefore(to.plusDays(1).atStartOfDay()));
        }

        return ledgerEntryRepository.findAll(spec, pageable)
                .map(entry -> new LedgerEntryResponse(
                        entry.getId(),
                        entry.getTransaction().getId(),
                        entry.getEntryType(),
                        entry.getAmount(),
                        entry.getBalanceAfter(),
                        entry.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public StatementResponse getStatement(UUID callerId, UUID walletId, LocalDate from, LocalDate to) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
        checkOwner(wallet, callerId);

        if (from.isAfter(to)) {
            throw new InvalidDateRangeException();
        }

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        Specification<LedgerEntry> inPeriod = LedgerEntrySpecifications.forWallet(walletId)
                .and(LedgerEntrySpecifications.createdFrom(start))
                .and(LedgerEntrySpecifications.createdBefore(end));
        List<LedgerEntry> entries = ledgerEntryRepository.findAll(inPeriod, Sort.by("createdAt").ascending());

        Specification<LedgerEntry> before = LedgerEntrySpecifications.forWallet(walletId)
                .and(LedgerEntrySpecifications.createdBefore(start));
        Pageable latestOnly = PageRequest.of(0, 1, Sort.by("createdAt").descending());
        BigDecimal openingBalance = ledgerEntryRepository.findAll(before, latestOnly)
                .getContent().stream()
                .findFirst()
                .map(LedgerEntry::getBalanceAfter)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalCredits = entries.stream()
                .filter(e -> e.getEntryType() == EntryType.CREDIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDebits = entries.stream()
                .filter(e -> e.getEntryType() == EntryType.DEBIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal closingBalance = openingBalance.add(totalCredits).subtract(totalDebits);

        List<LedgerEntryResponse> entryResponses = entries.stream()
                .map(e -> new LedgerEntryResponse(
                        e.getId(),
                        e.getTransaction().getId(),
                        e.getEntryType(),
                        e.getAmount(),
                        e.getBalanceAfter(),
                        e.getCreatedAt()))
                .toList();

        return new StatementResponse(walletId, wallet.getCurrency(), from, to,
                openingBalance, totalCredits, totalDebits, closingBalance, entryResponses);
    }

    private void checkOwner(Wallet wallet, UUID callerId) {
        if (!wallet.getUserId().equals(callerId)) {
            throw new WalletAccessDeniedException();
        }
    }
}