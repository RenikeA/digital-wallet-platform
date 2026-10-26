package com.walletplatform.walletservice.exception;

public class CurrencyMismatchException extends RuntimeException {
    public CurrencyMismatchException(String sourceCurrency,String destinationCurrency ) {
        super("Currency mismatch: " + sourceCurrency + " and " + destinationCurrency);
    }
}
