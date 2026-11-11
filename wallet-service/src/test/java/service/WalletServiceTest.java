package service;

import com.walletplatform.domain.*;
import com.walletplatform.repository.LedgerEntryRepository;
import com.walletplatform.repository.TransactionRepository;
import com.walletplatform.repository.WalletRepository;
import com.walletplatform.walletservice.dto.TransferRequest;
import com.walletplatform.walletservice.dto.WithdrawRequest;
import com.walletplatform.walletservice.event.WalletEventPublisher;
import com.walletplatform.walletservice.exception.CurrencyMismatchException;
import com.walletplatform.walletservice.exception.InsufficientFundsException;
import com.walletplatform.walletservice.exception.SameWalletTransferException;
import com.walletplatform.walletservice.exception.WalletAccessDeniedException;
import com.walletplatform.walletservice.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private LedgerEntryRepository ledgerEntryRepository;
    @Mock private WalletEventPublisher eventPublisher;

    @InjectMocks private WalletService walletService;

    @Test
    void withdraw_whenBalanceTooLow_throwsInsufficientFunds() {
        UUID callerId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        Wallet wallet = Wallet.builder()
                .id(walletId)
                .userId(callerId)
                .currency("NGN")
                .balance(new BigDecimal("100.00"))
                .status(WalletStatus.ACTIVE)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());

        WithdrawRequest request = new WithdrawRequest(walletId, new BigDecimal("500.00"), "key-1");

        assertThrows(InsufficientFundsException.class, () -> walletService.withdraw(callerId, request));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdraw_whenCallerIsNotOwner_throwsWalletAccessDenied() {
        UUID walletId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();

        Wallet wallet = Wallet.builder()
                .id(walletId)
                .userId(ownerId)
                .currency("NGN")
                .balance(new BigDecimal("1000.00"))
                .status(WalletStatus.ACTIVE)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        WithdrawRequest request = new WithdrawRequest(walletId, new BigDecimal("100.00"), "key-6");

        assertThrows(WalletAccessDeniedException.class, () -> walletService.withdraw(strangerId, request));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_whenSameWallet_throwsSameWalletTransferException() {
        UUID callerId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();

        TransferRequest request = new TransferRequest(walletId, walletId, new BigDecimal("100.00"), "key-2");

        assertThrows(SameWalletTransferException.class, () -> walletService.transfer(callerId, request));
    }

    @Test
    void transfer_whenCurrenciesDiffer_throwsCurrencyMismatchException() {
        UUID callerId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();

        Wallet sourceWallet = Wallet.builder()
                .id(sourceId)
                .userId(callerId)
                .currency("NGN")
                .balance(new BigDecimal("1000.00"))
                .status(WalletStatus.ACTIVE)
                .build();

        Wallet destinationWallet = Wallet.builder()
                .id(destinationId)
                .currency("USD")
                .balance(new BigDecimal("1000.00"))
                .status(WalletStatus.ACTIVE)
                .build();

        when(walletRepository.findById(sourceId)).thenReturn(Optional.of(sourceWallet));
        when(walletRepository.findById(destinationId)).thenReturn(Optional.of(destinationWallet));
        when(transactionRepository.findByIdempotencyKey("key-3")).thenReturn(Optional.empty());

        TransferRequest request = new TransferRequest(sourceId, destinationId, new BigDecimal("100.00"), "key-3");

        assertThrows(CurrencyMismatchException.class, () -> walletService.transfer(callerId, request));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_whenIdempotencyKeyAlreadyUsed_returnsExistingResultWithoutMovingMoneyAgain() {
        UUID callerId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();

        Wallet sourceWallet = Wallet.builder()
                .id(sourceId)
                .userId(callerId)
                .currency("NGN")
                .balance(new BigDecimal("1000.00"))
                .status(WalletStatus.ACTIVE)
                .build();

        Wallet destinationWallet = Wallet.builder()
                .id(destinationId)
                .currency("NGN")
                .balance(new BigDecimal("1000.00"))
                .status(WalletStatus.ACTIVE)
                .build();

        when(walletRepository.findById(sourceId)).thenReturn(Optional.of(sourceWallet));
        when(walletRepository.findById(destinationId)).thenReturn(Optional.of(destinationWallet));

        Transaction existingTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.TRANSFER)
                .idempotencyKey("key-4")
                .build();

        LedgerEntry debitEntry = LedgerEntry.builder()
                .transaction(existingTransaction)
                .wallet(sourceWallet)
                .amount(new BigDecimal("100.00"))
                .entryType(EntryType.DEBIT)
                .build();

        LedgerEntry creditEntry = LedgerEntry.builder()
                .transaction(existingTransaction)
                .wallet(destinationWallet)
                .amount(new BigDecimal("100.00"))
                .entryType(EntryType.CREDIT)
                .build();

        when(transactionRepository.findByIdempotencyKey("key-4"))
                .thenReturn(Optional.of(existingTransaction));
        when(ledgerEntryRepository.findByTransaction(existingTransaction))
                .thenReturn(List.of(debitEntry, creditEntry));

        TransferRequest request = new TransferRequest(sourceId, destinationId, new BigDecimal("100.00"), "key-4");

        Wallet result = walletService.transfer(callerId, request);

        assertEquals(0, new BigDecimal("1000.00").compareTo(result.getBalance()));
        verify(transactionRepository, never()).save(any());
        verify(ledgerEntryRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void transfer_whenValid_movesMoneyAndSavesOneTransactionAndTwoEntries() {
        UUID callerId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();

        Wallet sourceWallet = Wallet.builder()
                .id(sourceId)
                .userId(callerId)
                .currency("NGN")
                .balance(new BigDecimal("1000.00"))
                .status(WalletStatus.ACTIVE)
                .build();

        Wallet destinationWallet = Wallet.builder()
                .id(destinationId)
                .currency("NGN")
                .balance(new BigDecimal("500.00"))
                .status(WalletStatus.ACTIVE)
                .build();

        when(walletRepository.findById(sourceId)).thenReturn(Optional.of(sourceWallet));
        when(walletRepository.findById(destinationId)).thenReturn(Optional.of(destinationWallet));
        when(transactionRepository.findByIdempotencyKey("key-5")).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransferRequest request = new TransferRequest(sourceId, destinationId, new BigDecimal("100.00"), "key-5");

        Wallet result = walletService.transfer(callerId, request);

        assertEquals(0, new BigDecimal("900.00").compareTo(result.getBalance()));
        assertEquals(0, new BigDecimal("600.00").compareTo(destinationWallet.getBalance()));
        verify(transactionRepository, times(1)).save(any());
        verify(ledgerEntryRepository, times(2)).save(any());
        verify(walletRepository, times(2)).save(any());
        verify(eventPublisher, times(2)).publish(any());
    }
}