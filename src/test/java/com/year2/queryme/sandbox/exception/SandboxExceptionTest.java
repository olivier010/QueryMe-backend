package com.year2.queryme.sandbox.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Custom Sandbox Exception Tests")
class SandboxExceptionTest {

    @Test
    @DisplayName("SandboxProvisioningException should accept message")
    void sandboxProvisioningException_ShouldAcceptMessage() {
        String message = "Provisioning failed";
        SandboxProvisioningException exception = new SandboxProvisioningException(message);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("SandboxProvisioningException should accept message and cause")
    void sandboxProvisioningException_ShouldAcceptMessageAndCause() {
        String message = "Provisioning failed";
        Throwable cause = new RuntimeException("Database error");
        SandboxProvisioningException exception = new SandboxProvisioningException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("SandboxNotFoundException should accept message")
    void sandboxNotFoundException_ShouldAcceptMessage() {
        String message = "Sandbox not found";
        SandboxNotFoundException exception = new SandboxNotFoundException(message);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("SandboxNotFoundException should accept message and cause")
    void sandboxNotFoundException_ShouldAcceptMessageAndCause() {
        String message = "Sandbox not found";
        Throwable cause = new IllegalArgumentException("Invalid ID");
        SandboxNotFoundException exception = new SandboxNotFoundException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("SandboxExpiredException should accept message")
    void sandboxExpiredException_ShouldAcceptMessage() {
        String message = "Sandbox has expired";
        SandboxExpiredException exception = new SandboxExpiredException(message);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("SandboxExpiredException should accept message and cause")
    void sandboxExpiredException_ShouldAcceptMessageAndCause() {
        String message = "Sandbox has expired";
        Throwable cause = new IllegalStateException("Status check failed");
        SandboxExpiredException exception = new SandboxExpiredException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("All exceptions should extend RuntimeException")
    void allExceptions_ShouldExtendRuntimeException() {
        SandboxProvisioningException provisioningException = new SandboxProvisioningException("test");
        SandboxNotFoundException notFoundException = new SandboxNotFoundException("test");
        SandboxExpiredException expiredException = new SandboxExpiredException("test");

        assertInstanceOf(RuntimeException.class, provisioningException);
        assertInstanceOf(RuntimeException.class, notFoundException);
        assertInstanceOf(RuntimeException.class, expiredException);
    }
}
