package com.walletplatform.walletservice.exception;

import com.walletplatform.domain.WalletStatus;

public class WalletNotActiveException extends RuntimeException {
    public WalletNotActiveException(WalletStatus status) {
        super("Wallet is not active. Current status: " + status);
    }
}
