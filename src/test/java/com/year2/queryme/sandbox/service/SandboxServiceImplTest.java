package com.year2.queryme.sandbox.service;

import com.year2.queryme.sandbox.exception.SandboxExpiredException;
import com.year2.queryme.sandbox.exception.SandboxNotFoundException;
import com.year2.queryme.sandbox.exception.SandboxProvisioningException;
import com.year2.queryme.sandbox.model.SandboxRegistry;
import com.year2.queryme.sandbox.repository.SandboxRegistryRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SandboxServiceImpl Custom Exception Tests")
class SandboxServiceImplTest {

    @Mock
    private SandboxRegistryRepo registryRepo;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private SandboxServiceImpl sandboxService;

    private UUID examId;
    private UUID studentId;
    private String seedSql;

    @BeforeEach
    void setUp() {
        examId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        seedSql = "CREATE TABLE test (id INT); INSERT INTO test VALUES (1);";
    }

    @Test
    @DisplayName("Should throw SandboxProvisioningException when schema creation fails")
    void provisionSandbox_ShouldThrowSandboxProvisioningException_WhenSchemaCreationFails() {
        // Given
        when(jdbcTemplate.execute(anyString())).thenThrow(new DataAccessException("Schema creation failed") {});

        // When & Then
        SandboxProvisioningException exception = assertThrows(
                SandboxProvisioningException.class,
                () -> sandboxService.provisionSandbox(examId, studentId, seedSql)
        );

        assertEquals("Sandbox provisioning failed", exception.getMessage());
        assertNotNull(exception.getCause());
        verify(jdbcTemplate, atLeastOnce()).execute(contains("DROP SCHEMA IF EXISTS"));
        verify(jdbcTemplate, atLeastOnce()).execute(contains("DROP USER IF EXISTS"));
    }

    @Test
    @DisplayName("Should throw SandboxProvisioningException when user creation fails")
    void provisionSandbox_ShouldThrowSandboxProvisioningException_WhenUserCreationFails() {
        // Given
        when(jdbcTemplate.execute(anyString()))
                .thenReturn(null) // Schema creation succeeds
                .thenThrow(new DataAccessException("User creation failed") {}); // User creation fails

        // When & Then
        SandboxProvisioningException exception = assertThrows(
                SandboxProvisioningException.class,
                () -> sandboxService.provisionSandbox(examId, studentId, seedSql)
        );

        assertEquals("Sandbox provisioning failed", exception.getMessage());
        assertNotNull(exception.getCause());
    }

    @Test
    @DisplayName("Should throw SandboxProvisioningException when seed SQL execution fails")
    void provisionSandbox_ShouldThrowSandboxProvisioningException_WhenSeedSqlFails() {
        // Given
        when(jdbcTemplate.execute(anyString()))
                .thenReturn(null) // Schema creation succeeds
                .thenReturn(null) // User creation succeeds
                .thenReturn(null) // REVOKE succeeds
                .thenReturn(null) // GRANT USAGE succeeds
                .thenReturn(null) // GRANT SELECT succeeds
                .thenReturn(null) // ALTER DEFAULT PRIVILEGES succeeds
                .thenThrow(new DataAccessException("Seed SQL failed") {}); // SET search_path fails

        // When & Then
        SandboxProvisioningException exception = assertThrows(
                SandboxProvisioningException.class,
                () -> sandboxService.provisionSandbox(examId, studentId, seedSql)
        );

        assertEquals("Sandbox provisioning failed", exception.getMessage());
        assertNotNull(exception.getCause());
    }

    @Test
    @DisplayName("Should throw SandboxNotFoundException when sandbox doesn't exist during teardown")
    void teardownSandbox_ShouldThrowSandboxNotFoundException_WhenSandboxNotFound() {
        // Given
        when(registryRepo.findByExamIdAndStudentId(examId, studentId))
                .thenReturn(Optional.empty());

        // When & Then
        SandboxNotFoundException exception = assertThrows(
                SandboxNotFoundException.class,
                () -> sandboxService.teardownSandbox(examId, studentId)
        );

        assertEquals("Sandbox not found for given Exam and Student", exception.getMessage());
        verify(registryRepo).findByExamIdAndStudentId(examId, studentId);
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    @DisplayName("Should throw SandboxNotFoundException when getting connection details for non-existent sandbox")
    void getSandboxConnectionDetails_ShouldThrowSandboxNotFoundException_WhenSandboxNotFound() {
        // Given
        when(registryRepo.findByExamIdAndStudentId(examId, studentId))
                .thenReturn(Optional.empty());

        // When & Then
        SandboxNotFoundException exception = assertThrows(
                SandboxNotFoundException.class,
                () -> sandboxService.getSandboxConnectionDetails(examId, studentId)
        );

        assertEquals("Sandbox not found", exception.getMessage());
        verify(registryRepo).findByExamIdAndStudentId(examId, studentId);
    }

    @Test
    @DisplayName("Should throw SandboxExpiredException when sandbox is not active")
    void getSandboxConnectionDetails_ShouldThrowSandboxExpiredException_WhenSandboxNotActive() {
        // Given
        SandboxRegistry inactiveRegistry = new SandboxRegistry();
        inactiveRegistry.setExamId(examId);
        inactiveRegistry.setStudentId(studentId);
        inactiveRegistry.setSchemaName("test_schema");
        inactiveRegistry.setDbUser("test_user");
        inactiveRegistry.setStatus("DROPPED"); // Not ACTIVE

        when(registryRepo.findByExamIdAndStudentId(examId, studentId))
                .thenReturn(Optional.of(inactiveRegistry));

        // When & Then
        SandboxExpiredException exception = assertThrows(
                SandboxExpiredException.class,
                () -> sandboxService.getSandboxConnectionDetails(examId, studentId)
        );

        assertEquals("Sandbox is not active.", exception.getMessage());
        verify(registryRepo).findByExamIdAndStudentId(examId, studentId);
    }

    @Test
    @DisplayName("Should throw SandboxExpiredException when sandbox status is null")
    void getSandboxConnectionDetails_ShouldThrowSandboxExpiredException_WhenSandboxStatusIsNull() {
        // Given
        SandboxRegistry nullStatusRegistry = new SandboxRegistry();
        nullStatusRegistry.setExamId(examId);
        nullStatusRegistry.setStudentId(studentId);
        nullStatusRegistry.setSchemaName("test_schema");
        nullStatusRegistry.setDbUser("test_user");
        nullStatusRegistry.setStatus(null); // Null status

        when(registryRepo.findByExamIdAndStudentId(examId, studentId))
                .thenReturn(Optional.of(nullStatusRegistry));

        // When & Then
        SandboxExpiredException exception = assertThrows(
                SandboxExpiredException.class,
                () -> sandboxService.getSandboxConnectionDetails(examId, studentId)
        );

        assertEquals("Sandbox is not active.", exception.getMessage());
    }

    @Test
    @DisplayName("Should successfully provision sandbox when no exceptions occur")
    void provisionSandbox_ShouldSucceed_WhenNoErrors() {
        // Given
        when(jdbcTemplate.execute(anyString())).thenReturn(null);
        when(registryRepo.save(any(SandboxRegistry.class))).thenReturn(new SandboxRegistry());

        // When & Then
        assertDoesNotThrow(() -> {
            String result = sandboxService.provisionSandbox(examId, studentId, seedSql);
            assertNotNull(result);
            assertTrue(result.contains("exam_"));
            assertTrue(result.contains("student_"));
        });

        verify(jdbcTemplate, atLeastOnce()).execute(anyString());
        verify(registryRepo).save(any(SandboxRegistry.class));
    }

    @Test
    @DisplayName("Should successfully get connection details for active sandbox")
    void getSandboxConnectionDetails_ShouldSucceed_WhenSandboxIsActive() {
        // Given
        SandboxRegistry activeRegistry = new SandboxRegistry();
        activeRegistry.setExamId(examId);
        activeRegistry.setStudentId(studentId);
        activeRegistry.setSchemaName("test_schema");
        activeRegistry.setDbUser("test_user");
        activeRegistry.setStatus("ACTIVE");

        when(registryRepo.findByExamIdAndStudentId(examId, studentId))
                .thenReturn(Optional.of(activeRegistry));

        // When & Then
        assertDoesNotThrow(() -> {
            var result = sandboxService.getSandboxConnectionDetails(examId, studentId);
            assertNotNull(result);
            assertEquals("test_schema", result.schemaName());
            assertEquals("test_user", result.dbUser());
        });
    }
}
