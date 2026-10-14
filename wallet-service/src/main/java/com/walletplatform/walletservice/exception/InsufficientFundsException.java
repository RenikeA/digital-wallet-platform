package com.walletplatform.walletservice.exception;

import com.walletplatform.domain.WalletStatus;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(BigDecimal balance, BigDecimal amount) {
        super("Insufficient funds. Balance: " + balance + ", requested: " + amount);
    }
}
