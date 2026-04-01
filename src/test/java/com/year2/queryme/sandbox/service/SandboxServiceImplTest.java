package com.year2.queryme.sandbox.service;

import com.year2.queryme.sandbox.dto.SandboxConnectionInfo;
import com.year2.queryme.sandbox.model.SandboxRegistry;
import com.year2.queryme.sandbox.repository.SandboxRegistryRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
    private String schemaName;
    private String dbUser;
    private String dbPassword;

    @BeforeEach
    void setUp() {
        examId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        seedSql = "CREATE TABLE test (id INT PRIMARY KEY); INSERT INTO test VALUES (1);";
        schemaName = "exam_" + examId.toString().replace("-", "") + "_student_" + studentId.toString().replace("-", "");
        dbUser = "usr_" + schemaName.substring(0, Math.min(schemaName.length(), 50));
        dbPassword = UUID.randomUUID().toString();
    }

    @Test
    void testProvisionSandbox_Success() {
        // Given
        when(registryRepo.save(any(SandboxRegistry.class))).thenAnswer(invocation -> {
            SandboxRegistry registry = invocation.getArgument(0);
            registry.setId(UUID.randomUUID());
            return registry;
        });

        // When
        String result = sandboxService.provisionSandbox(examId, studentId, seedSql);

        // Then
        assertEquals(schemaName, result);

        // Verify schema creation
        verify(jdbcTemplate).execute("CREATE SCHEMA " + schemaName);
        
        // Verify user creation
        ArgumentCaptor<String> userCreationCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).execute(userCreationCaptor.capture());
        assertTrue(userCreationCaptor.getValue().contains("CREATE USER " + dbUser));
        assertTrue(userCreationCaptor.getValue().contains("WITH PASSWORD"));

        // Verify security restrictions
        verify(jdbcTemplate).execute("REVOKE ALL ON SCHEMA public FROM " + dbUser);
        verify(jdbcTemplate).execute("GRANT USAGE ON SCHEMA " + schemaName + " TO " + dbUser);
        verify(jdbcTemplate).execute("GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA " + schemaName + " TO " + dbUser);

        // Verify seed SQL execution
        verify(jdbcTemplate).execute("SET search_path TO " + schemaName);
        verify(jdbcTemplate).execute(seedSql);
        verify(jdbcTemplate).execute("SET search_path TO public");

        // Verify registry save
        ArgumentCaptor<SandboxRegistry> registryCaptor = ArgumentCaptor.forClass(SandboxRegistry.class);
        verify(registryRepo).save(registryCaptor.capture());
        SandboxRegistry savedRegistry = registryCaptor.getValue();
        assertEquals(examId, savedRegistry.getExamId());
        assertEquals(studentId, savedRegistry.getStudentId());
        assertEquals(schemaName, savedRegistry.getSchemaName());
        assertEquals(dbUser, savedRegistry.getDbUser());
        assertEquals("ACTIVE", savedRegistry.getStatus());
    }

    @Test
    void testProvisionSandbox_WithNullSeedSql() {
        // Given
        when(registryRepo.save(any(SandboxRegistry.class))).thenReturn(new SandboxRegistry());

        // When
        String result = sandboxService.provisionSandbox(examId, studentId, null);

        // Then
        assertEquals(schemaName, result);

        // Verify schema and user creation still happen
        verify(jdbcTemplate).execute("CREATE SCHEMA " + schemaName);
        verify(jdbcTemplate).execute(contains("CREATE USER " + dbUser));

        // Verify seed SQL is NOT executed
        verify(jdbcTemplate, never()).execute(seedSql);
        verify(jdbcTemplate, times(2)).execute(contains("search_path")); // Only for setting and resetting
    }

    @Test
    void testProvisionSandbox_WithEmptySeedSql() {
        // Given
        when(registryRepo.save(any(SandboxRegistry.class))).thenReturn(new SandboxRegistry());

        // When
        String result = sandboxService.provisionSandbox(examId, studentId, "   ");

        // Then
        assertEquals(schemaName, result);

        // Verify seed SQL is NOT executed for empty SQL
        verify(jdbcTemplate, never()).execute("   ");
        verify(jdbcTemplate, times(2)).execute(contains("search_path"));
    }

    @Test
    void testProvisionSandbox_Failure_RollsBack() {
        // Given
        when(jdbcTemplate.execute(anyString())).thenThrow(new RuntimeException("Database error"));
        when(registryRepo.save(any(SandboxRegistry.class))).thenReturn(new SandboxRegistry());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            sandboxService.provisionSandbox(examId, studentId, seedSql);
        });

        // Verify cleanup on failure
        verify(jdbcTemplate).execute("DROP SCHEMA IF EXISTS " + schemaName + " CASCADE");
        verify(jdbcTemplate).execute("DROP USER IF EXISTS " + dbUser);
    }

    @Test
    void testTeardownSandbox_Success() {
        // Given
        SandboxRegistry registry = new SandboxRegistry();
        registry.setExamId(examId);
        registry.setStudentId(studentId);
        registry.setSchemaName(schemaName);
        registry.setDbUser(dbUser);
        registry.setStatus("ACTIVE");

        when(registryRepo.findByExamIdAndStudentId(examId, studentId))
                .thenReturn(Optional.of(registry));
        when(registryRepo.save(any(SandboxRegistry.class))).thenReturn(registry);

        // When
        sandboxService.teardownSandbox(examId, studentId);

        // Then
        verify(jdbcTemplate).execute("DROP SCHEMA IF EXISTS " + schemaName + " CASCADE");
        verify(jdbcTemplate).execute("DROP USER IF EXISTS " + dbUser);

        // Verify registry update
        ArgumentCaptor<SandboxRegistry> registryCaptor = ArgumentCaptor.forClass(SandboxRegistry.class);
        verify(registryRepo).save(registryCaptor.capture());
        SandboxRegistry updatedRegistry = registryCaptor.getValue();
        assertEquals("DROPPED", updatedRegistry.getStatus());
    }

    @Test
    void testTeardownSandbox_NotFound() {
        // Given
        when(registryRepo.findByExamIdAndStudentId(examId, studentId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            sandboxService.teardownSandbox(examId, studentId);
        });

        // Verify no cleanup operations
        verify(jdbcTemplate, never()).execute(anyString());
        verify(registryRepo, never()).save(any());
    }

    @Test
    void testGetSandboxConnectionDetails_Success() {
        // Given
        SandboxRegistry registry = new SandboxRegistry();
        registry.setExamId(examId);
        registry.setStudentId(studentId);
        registry.setSchemaName(schemaName);
        registry.setDbUser(dbUser);
        registry.setStatus("ACTIVE");

        when(registryRepo.findByExamIdAndStudentId(examId, studentId))
                .thenReturn(Optional.of(registry));

        // When
        SandboxConnectionInfo result = sandboxService.getSandboxConnectionDetails(examId, studentId);

        // Then
        assertNotNull(result);
        assertEquals(schemaName, result.getSchemaName());
        assertEquals(dbUser, result.getDbUser());
    }

    @Test
    void testGetSandboxConnectionDetails_NotFound() {
        // Given
        when(registryRepo.findByExamIdAndStudentId(examId, studentId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            sandboxService.getSandboxConnectionDetails(examId, studentId);
        });
    }

    @Test
    void testGetSandboxConnectionDetails_Inactive() {
        // Given
        SandboxRegistry registry = new SandboxRegistry();
        registry.setExamId(examId);
        registry.setStudentId(studentId);
        registry.setSchemaName(schemaName);
        registry.setDbUser(dbUser);
        registry.setStatus("DROPPED");

        when(registryRepo.findByExamIdAndStudentId(examId, studentId))
                .thenReturn(Optional.of(registry));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            sandboxService.getSandboxConnectionDetails(examId, studentId);
        });
    }

    @Test
    void testProvisionSandbox_GeneratesUniqueSchemaName() {
        // Given
        UUID differentExamId = UUID.randomUUID();
        UUID differentStudentId = UUID.randomUUID();
        String expectedSchemaName = "exam_" + differentExamId.toString().replace("-", "") + 
                                  "_student_" + differentStudentId.toString().replace("-", "");
        
        when(registryRepo.save(any(SandboxRegistry.class))).thenReturn(new SandboxRegistry());

        // When
        String result = sandboxService.provisionSandbox(differentExamId, differentStudentId, seedSql);

        // Then
        assertEquals(expectedSchemaName, result);
        assertNotEquals(schemaName, result); // Should be different from the original
    }
}
