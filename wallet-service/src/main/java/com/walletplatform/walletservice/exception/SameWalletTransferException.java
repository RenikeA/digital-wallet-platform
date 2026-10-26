package com.walletplatform.walletservice.exception;

public class SameWalletTransferException extends RuntimeException {
    public SameWalletTransferException() {
        super("Source and destination wallets must be different");
    }
}
