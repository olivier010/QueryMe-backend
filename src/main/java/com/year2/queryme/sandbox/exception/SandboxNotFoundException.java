package com.year2.queryme.sandbox.exception;

public class SandboxNotFoundException extends RuntimeException {
    public SandboxNotFoundException(String message) {
        super(message);
    }

    public SandboxNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
