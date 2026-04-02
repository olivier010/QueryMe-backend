package com.year2.queryme.sandbox.exception;

public class SandboxProvisioningException extends RuntimeException {
    public SandboxProvisioningException(String message) {
        super(message);
    }

    public SandboxProvisioningException(String message, Throwable cause) {
        super(message, cause);
    }
}
