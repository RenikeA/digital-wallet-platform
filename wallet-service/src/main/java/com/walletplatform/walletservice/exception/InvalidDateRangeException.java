package com.walletplatform.walletservice.exception;

public class InvalidDateRangeException extends RuntimeException {

    public InvalidDateRangeException() {
        super("The 'from' date must not be after the 'to' date");
    }
}
