package com.year2.queryme.sandbox.service;

import com.year2.queryme.sandbox.dto.SandboxConnectionInfo;
import com.year2.queryme.repository.ExamRepository;
import com.year2.queryme.repository.StudentRepository;
import com.year2.queryme.repository.UserRepository;
import com.year2.queryme.sandbox.model.SandboxRegistry;
import com.year2.queryme.sandbox.repository.SandboxRegistryRepo;
import com.year2.queryme.sandbox.exception.SandboxExpiredException;
import com.year2.queryme.sandbox.exception.SandboxNotFoundException;
import com.year2.queryme.sandbox.exception.SandboxProvisioningException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.beans.factory.annotation.Value;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SandboxServiceImpl implements SandboxService {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]{0,62}$");
    private static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_-+=[]{}";

    private final SandboxRegistryRepo registryRepo;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final JdbcTemplate jdbcTemplate;
    private final String dbUsername;
    private final boolean dbUserIsolationEnabled;
    private final String sandboxDbUserPrefix;
    private final int sandboxDbPasswordLength;
    private final SecureRandom secureRandom = new SecureRandom();

    // Uses the standard Spring Boot database connection automatically
    public SandboxServiceImpl(
            SandboxRegistryRepo registryRepo,
            ExamRepository examRepository,
            UserRepository userRepository,
            StudentRepository studentRepository,
            @Qualifier("sandboxJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Value("${queryme.sandbox-datasource.username:${spring.datasource.username}}") String dbUsername,
            @Value("${queryme.sandbox.security.db-user-isolation.enabled:false}") boolean dbUserIsolationEnabled,
            @Value("${queryme.sandbox.security.db-user-isolation.user-prefix:qb_}") String sandboxDbUserPrefix,
            @Value("${queryme.sandbox.security.db-user-isolation.password-length:24}") int sandboxDbPasswordLength
    ) {
        this.registryRepo = registryRepo;
        this.examRepository = examRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.dbUsername = dbUsername;
        this.dbUserIsolationEnabled = dbUserIsolationEnabled;
        this.sandboxDbUserPrefix = sandboxDbUserPrefix;
        this.sandboxDbPasswordLength = Math.max(16, sandboxDbPasswordLength);
    }

    @Override
    @Transactional
    public String provisionSandbox(UUID examId, UUID studentId, String seedSql) {
        SandboxRegistry existingRegistry = registryRepo.findByExamIdAndStudentId(examId, studentId)
                .filter(registry -> "ACTIVE".equals(registry.getStatus()))
                .orElse(null);

        if (existingRegistry != null) {
            log.info("Reusing existing active sandbox schema: {}", existingRegistry.getSchemaName());
            return existingRegistry.getSchemaName();
        }

        var exam = examRepository.findById(examId.toString())
                .orElseThrow(() -> new SandboxProvisioningException("Exam not found in registry"));

        userRepository.findById(studentId)
                .orElseThrow(() -> new SandboxProvisioningException("Student not found in Auth registry"));

        String studentNumber = studentRepository.findByUser_Id(studentId)
                .map(student -> student.getStudentNumber())
                .filter(number -> number != null && !number.isBlank())
                .orElse("u" + studentId.toString().replace("-", "").substring(0, 8));

        String safeStudentNumber = studentNumber.replaceAll("[^a-zA-Z0-9]", "");

        String schemaName = "exam_%s_student_%s".formatted(
                examId.toString().replace("-", "").substring(0, 8),
                safeStudentNumber);
        schemaName = requireSafeIdentifier(schemaName, "schemaName");

        log.info("Provisioning sandbox schema: {}", schemaName);
        String sandboxDbUser = dbUsername;
        boolean dedicatedRoleCreated = false;

        try {
            jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);

            if (seedSql != null && !seedSql.trim().isEmpty()) {
                jdbcTemplate.execute("SET search_path TO " + schemaName);
                try {
                    jdbcTemplate.execute(seedSql);
                } finally {
                    jdbcTemplate.execute("SET search_path TO public");
                }
            }

            if (dbUserIsolationEnabled) {
                String dedicatedDbUser = buildSandboxDbUser(examId, studentId);
                String dedicatedDbPassword = generateSecurePassword();

                if (isPostgreSql()) {
                    ensureDedicatedRoleDropped(dedicatedDbUser);
                    createDedicatedRole(dedicatedDbUser, dedicatedDbPassword, schemaName);
                    dedicatedRoleCreated = true;
                    sandboxDbUser = dedicatedDbUser;
                } else {
                    log.warn("Sandbox DB user isolation is enabled but database is not PostgreSQL; skipping dedicated role provisioning");
                }
            }

            SandboxRegistry registry = new SandboxRegistry();
            registry.setExamId(examId);
            registry.setStudentId(studentId);
            registry.setSchemaName(schemaName);
            registry.setDbUser(sandboxDbUser);
            registry.setStatus("ACTIVE");
            registry.setExpiresAt(exam.getTimeLimitMins() != null
                    ? LocalDateTime.now().plusMinutes(exam.getTimeLimitMins())
                    : null);
            registryRepo.save(registry);

            return schemaName;

        } catch (Exception e) {
            log.error("Failed to provision sandbox: {}", schemaName, e);
            try {
                jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + schemaName + " CASCADE");
            } catch (Exception cleanupEx) {
                log.warn("Failed to cleanup schema {} after provisioning error: {}", schemaName, cleanupEx.getMessage());
            }

            if (dedicatedRoleCreated && sandboxDbUser != null) {
                try {
                    ensureDedicatedRoleDropped(sandboxDbUser);
                } catch (Exception cleanupEx) {
                    log.warn("Failed to cleanup role {} after provisioning error: {}", sandboxDbUser, cleanupEx.getMessage());
                }
            }
            throw new SandboxProvisioningException("Sandbox provisioning failed", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void teardownSandbox(UUID examId, UUID studentId) {
        SandboxRegistry registry = registryRepo.findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new SandboxNotFoundException("Sandbox not found for given Exam and Student"));

        String schemaName = requireSafeIdentifier(registry.getSchemaName(), "schemaName");

        log.info("Tearing down sandbox schema: {}", schemaName);

        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + schemaName + " CASCADE");

        if (dbUserIsolationEnabled && registry.getDbUser() != null && registry.getDbUser().startsWith(sandboxDbUserPrefix) && isPostgreSql()) {
            try {
                ensureDedicatedRoleDropped(registry.getDbUser());
            } catch (Exception ex) {
                log.warn("Failed to drop dedicated sandbox role {}: {}", registry.getDbUser(), ex.getMessage());
            }
        }

        registry.setStatus("DROPPED");
        registryRepo.save(registry);
    }

    @Override
    public SandboxConnectionInfo getSandboxConnectionDetails(UUID examId, UUID studentId) {
        SandboxRegistry registry = registryRepo.findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new SandboxNotFoundException("Sandbox not found"));

        if (!"ACTIVE".equals(registry.getStatus())) {
            throw new SandboxExpiredException("Sandbox is not active.");
        }

        return new SandboxConnectionInfo(registry.getSchemaName(), registry.getDbUser());
    }

    private String requireSafeIdentifier(String identifier, String fieldName) {
        if (identifier == null || !IDENTIFIER_PATTERN.matcher(identifier).matches()) {
            throw new SandboxProvisioningException("Unsafe " + fieldName + " provided");
        }
        return identifier;
    }

    private boolean isPostgreSql() {
        String productName = jdbcTemplate.execute((java.sql.Connection con) -> con.getMetaData().getDatabaseProductName());
        return productName != null && productName.toLowerCase().contains("postgresql");
    }

    private String buildSandboxDbUser(UUID examId, UUID studentId) {
        String generated = (sandboxDbUserPrefix
                + examId.toString().replace("-", "").substring(0, 8)
                + "_"
                + studentId.toString().replace("-", "").substring(0, 8))
                .toLowerCase();

        if (generated.length() > 63) {
            generated = generated.substring(0, 63);
        }

        return requireSafeIdentifier(generated, "dbUser");
    }

    private void createDedicatedRole(String dbUser, String password, String schemaName) {
        String escapedPassword = escapeSqlLiteral(password);
        jdbcTemplate.execute("CREATE ROLE " + dbUser + " LOGIN PASSWORD '" + escapedPassword + "'");
        jdbcTemplate.execute("ALTER ROLE " + dbUser + " NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT");
        jdbcTemplate.execute("ALTER ROLE " + dbUser + " SET search_path = " + schemaName);
        jdbcTemplate.execute("REVOKE ALL ON SCHEMA public FROM " + dbUser);
        jdbcTemplate.execute("GRANT USAGE ON SCHEMA " + schemaName + " TO " + dbUser);
        jdbcTemplate.execute("GRANT SELECT ON ALL TABLES IN SCHEMA " + schemaName + " TO " + dbUser);
        jdbcTemplate.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA " + schemaName + " TO " + dbUser);
        jdbcTemplate.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA " + schemaName + " GRANT SELECT ON TABLES TO " + dbUser);
        jdbcTemplate.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA " + schemaName + " GRANT USAGE, SELECT ON SEQUENCES TO " + dbUser);
    }

    private void ensureDedicatedRoleDropped(String dbUser) {
        String safeDbUser = requireSafeIdentifier(dbUser, "dbUser");
        jdbcTemplate.execute("DROP ROLE IF EXISTS " + safeDbUser);
    }

    private String generateSecurePassword() {
        StringBuilder password = new StringBuilder(sandboxDbPasswordLength);
        for (int i = 0; i < sandboxDbPasswordLength; i++) {
            int index = secureRandom.nextInt(PASSWORD_CHARS.length());
            password.append(PASSWORD_CHARS.charAt(index));
        }
        return password.toString();
    }

    private String escapeSqlLiteral(String value) {
        return value == null ? "" : value.replace("'", "''");
    }
}
