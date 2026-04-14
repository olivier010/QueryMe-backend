package com.year2.queryme.sandbox.dto;

public record SandboxProvisionRequest(
        java.util.UUID examId,
        Long studentId,
        String seedSql
) {
}

