package com.zaxxer.hikari.pool;

public class FailFastException extends RuntimeException {
    public FailFastException(String message) {
        super(message);
    }

    public FailFastException(String message, Throwable thr) {
        super(message, thr);
    }
}
