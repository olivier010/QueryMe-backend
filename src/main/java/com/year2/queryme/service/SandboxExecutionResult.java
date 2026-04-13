package com.year2.queryme.service;

import java.util.List;
import java.util.Map;

public record SandboxExecutionResult(
        boolean hasResultSet,
        List<String> columns,
        List<Map<String, Object>> rows
) {
}
