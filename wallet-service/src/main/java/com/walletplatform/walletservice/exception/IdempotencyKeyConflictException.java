package com.walletplatform.walletservice.exception;

public class IdempotencyKeyConflictException extends RuntimeException {
    public IdempotencyKeyConflictException(String idempotencyKey) {
        super("Idempotency key '" + idempotencyKey + "' was already used for a different request");
    }
}
