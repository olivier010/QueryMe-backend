package com.year2.queryme.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class QueryValidator {

    private static final String IDENTIFIER = "(?:\"[^\"]+\"|[A-Za-z_][A-Za-z0-9_$]*)";
    private static final String QUALIFIED_IDENTIFIER = IDENTIFIER + "(?:\\s*\\.\\s*" + IDENTIFIER + ")?";

    private static final Pattern COMMENT_PATTERN = Pattern.compile("(?s)(--|/\\*|\\*/)");
    private static final Pattern DOLLAR_QUOTE_PATTERN = Pattern.compile("\\$\\$");
    private static final Pattern LEADING_KEYWORD_PATTERN = Pattern.compile("^([A-Z]+)");
    private static final Pattern DISALLOWED_GLOBAL_COMMANDS = Pattern.compile(
            "\\b(GRANT|REVOKE|CALL|DO|COPY|VACUUM|ANALYZE|LISTEN|UNLISTEN|NOTIFY)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DISALLOWED_OBJECT_COMMANDS = Pattern.compile(
            "\\b(?:CREATE|ALTER|DROP)\\s+(?:OR\\s+REPLACE\\s+)?"
                    + "(SCHEMA|DATABASE|ROLE|USER|FUNCTION|PROCEDURE|EXTENSION|TABLESPACE|SERVER|TRIGGER|TYPE)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DISALLOWED_ALTER_TABLE_ACTIONS = Pattern.compile(
            "\\bSET\\s+SCHEMA\\b|\\bOWNER\\s+TO\\b|\\bSET\\s+TABLESPACE\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TEMP_OBJECT_PATTERN = Pattern.compile("\\b(TEMP|TEMPORARY|UNLOGGED)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREATE_ALLOWED_PATTERN = Pattern.compile(
            "^CREATE\\s+(?:(?:OR\\s+REPLACE\\s+)?VIEW\\b|TABLE\\b|(?:UNIQUE\\s+)?INDEX\\b)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ALTER_ALLOWED_PATTERN = Pattern.compile("^ALTER\\s+TABLE\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DROP_ALLOWED_PATTERN = Pattern.compile("^DROP\\s+(TABLE|VIEW|INDEX)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern RESULT_SET_PATTERN = Pattern.compile("^(SELECT|WITH)\\b|\\bRETURNING\\b", Pattern.CASE_INSENSITIVE);

    private static final List<Pattern> RELATION_REFERENCE_PATTERNS = List.of(
            Pattern.compile("(?i)\\b(?:FROM|JOIN|USING)\\s+(" + QUALIFIED_IDENTIFIER + ")"),
            Pattern.compile("(?i)\\bINSERT\\s+INTO\\s+(" + QUALIFIED_IDENTIFIER + ")"),
            Pattern.compile("(?i)\\bUPDATE\\s+(" + QUALIFIED_IDENTIFIER + ")"),
            Pattern.compile("(?i)\\bDELETE\\s+FROM\\s+(" + QUALIFIED_IDENTIFIER + ")"),
            Pattern.compile("(?i)\\bTRUNCATE\\s+(?:TABLE\\s+)?(" + QUALIFIED_IDENTIFIER + ")"),
            Pattern.compile("(?i)\\bCREATE\\s+(?:OR\\s+REPLACE\\s+)?TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(" + QUALIFIED_IDENTIFIER + ")"),
            Pattern.compile("(?i)\\bALTER\\s+TABLE\\s+(?:ONLY\\s+)?(" + QUALIFIED_IDENTIFIER + ")"),
            Pattern.compile("(?i)\\bDROP\\s+TABLE\\s+(?:IF\\s+EXISTS\\s+)?(" + QUALIFIED_IDENTIFIER + ")"),
            Pattern.compile("(?i)\\bCREATE\\s+(?:OR\\s+REPLACE\\s+)?VIEW\\s+(" + QUALIFIED_IDENTIFIER + ")"),
            Pattern.compile("(?i)\\bDROP\\s+VIEW\\s+(?:IF\\s+EXISTS\\s+)?(" + QUALIFIED_IDENTIFIER + ")"),
            Pattern.compile("(?i)\\bCREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+(?:CONCURRENTLY\\s+)?(?:IF\\s+NOT\\s+EXISTS\\s+)?"
                    + IDENTIFIER + "\\s+ON\\s+(" + QUALIFIED_IDENTIFIER + ")"),
            Pattern.compile("(?i)\\bDROP\\s+INDEX\\s+(?:CONCURRENTLY\\s+)?(?:IF\\s+EXISTS\\s+)?(" + QUALIFIED_IDENTIFIER + ")")
    );

    public void validate(String query) {
        validate(query, null, false);
    }

    public void validate(String query, String sandboxSchema, boolean requireResultSet) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be empty");
        }

        String trimmedQuery = query.trim();
        if (COMMENT_PATTERN.matcher(trimmedQuery).find()) {
            throw new IllegalArgumentException("SQL comments are not allowed in submissions");
        }

        if (DOLLAR_QUOTE_PATTERN.matcher(trimmedQuery).find()) {
            throw new IllegalArgumentException("Dollar-quoted SQL blocks are not allowed");
        }

        List<String> statements = SqlScriptUtils.splitStatements(trimmedQuery);
        if (statements.isEmpty()) {
            throw new IllegalArgumentException("Query cannot be empty");
        }

        for (int i = 0; i < statements.size(); i++) {
            validateStatement(statements.get(i), sandboxSchema, requireResultSet && i == statements.size() - 1);
        }
    }

    private void validateStatement(String statement, String sandboxSchema, boolean requireResultSet) {
        String normalized = SqlScriptUtils.maskSingleQuotedLiterals(statement).trim();
        String leadingKeyword = extractLeadingKeyword(normalized);

        if (leadingKeyword == null) {
            throw new IllegalArgumentException("SQL statement is missing a command");
        }

        if (DISALLOWED_GLOBAL_COMMANDS.matcher(normalized).find()) {
            throw new IllegalArgumentException("System-level SQL commands are not allowed");
        }

        if (DISALLOWED_OBJECT_COMMANDS.matcher(normalized).find()) {
            throw new IllegalArgumentException("Schema-wide or server-wide SQL objects are not allowed");
        }

        if (TEMP_OBJECT_PATTERN.matcher(normalized).find()) {
            throw new IllegalArgumentException("Temporary or unlogged SQL objects are not allowed");
        }

        switch (leadingKeyword) {
            case "SELECT", "WITH", "INSERT", "UPDATE", "DELETE", "TRUNCATE" -> {
                // allowed
            }
            case "CREATE" -> {
                if (!CREATE_ALLOWED_PATTERN.matcher(normalized).find()) {
                    throw new IllegalArgumentException("Only CREATE TABLE, CREATE VIEW, and CREATE INDEX are allowed");
                }
            }
            case "ALTER" -> {
                if (!ALTER_ALLOWED_PATTERN.matcher(normalized).find()) {
                    throw new IllegalArgumentException("Only ALTER TABLE is allowed");
                }
                if (DISALLOWED_ALTER_TABLE_ACTIONS.matcher(normalized).find()) {
                    throw new IllegalArgumentException("ALTER TABLE cannot move ownership or schema scope");
                }
            }
            case "DROP" -> {
                if (!DROP_ALLOWED_PATTERN.matcher(normalized).find()) {
                    throw new IllegalArgumentException("Only DROP TABLE, DROP VIEW, and DROP INDEX are allowed");
                }
            }
            default -> throw new IllegalArgumentException("SQL command not allowed: " + leadingKeyword);
        }

        if (("DROP".equals(leadingKeyword) || "TRUNCATE".equals(leadingKeyword)) && normalized.contains(",")) {
            throw new IllegalArgumentException("Only one relation target is allowed per DROP or TRUNCATE statement");
        }

        validateRelationScope(normalized, sandboxSchema);

        if (requireResultSet && !RESULT_SET_PATTERN.matcher(normalized).find()) {
            throw new IllegalArgumentException("The final reference statement must return a result set");
        }
    }

    private void validateRelationScope(String statement, String sandboxSchema) {
        for (Pattern pattern : RELATION_REFERENCE_PATTERNS) {
            var matcher = pattern.matcher(statement);
            while (matcher.find()) {
                String reference = matcher.group(1);
                if (reference == null || reference.startsWith("(")) {
                    continue;
                }

                String[] parts = reference.replaceAll("\\s+", "").split("\\.");
                if (parts.length != 2) {
                    continue;
                }

                String schemaName = normalizeIdentifier(parts[0]);
                if (schemaName.isBlank()) {
                    continue;
                }

                if (isSystemSchema(schemaName)) {
                    throw new IllegalArgumentException("Cross-schema access is not allowed: " + schemaName);
                }

                if (sandboxSchema != null && !schemaName.equals(normalizeIdentifier(sandboxSchema))) {
                    throw new IllegalArgumentException("Cross-sandbox access is not allowed: " + schemaName);
                }

                if (sandboxSchema == null && schemaName.startsWith("exam_")) {
                    throw new IllegalArgumentException("Explicit sandbox schema references are not allowed");
                }
            }
        }
    }

    private String extractLeadingKeyword(String statement) {
        var matcher = LEADING_KEYWORD_PATTERN.matcher(statement.toUpperCase(Locale.ROOT));
        return matcher.find() ? matcher.group(1) : null;
    }

    private String normalizeIdentifier(String identifier) {
        String trimmed = identifier == null ? "" : identifier.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private boolean isSystemSchema(String schemaName) {
        return "public".equals(schemaName)
                || "information_schema".equals(schemaName)
                || schemaName.startsWith("pg_");
    }
}
