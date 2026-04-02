package com.year2.queryme.sandbox.exception;

public class SandboxExpiredException extends RuntimeException {
    public SandboxExpiredException(String message) {
        super(message);
    }

    public SandboxExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
