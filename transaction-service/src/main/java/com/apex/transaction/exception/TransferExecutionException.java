package com.apex.transaction.exception;

public class TransferExecutionException extends RuntimeException {
    public TransferExecutionException(String message) {
        super(message);
    }

    public TransferExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
