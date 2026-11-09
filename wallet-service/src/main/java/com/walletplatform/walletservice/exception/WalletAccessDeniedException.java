package com.walletplatform.walletservice.exception;

public class WalletAccessDeniedException extends RuntimeException {
    public WalletAccessDeniedException() {

        super("You do not have access to this wallet");
    }
}
