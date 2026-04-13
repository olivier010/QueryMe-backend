package com.year2.queryme.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class QueryExecutor {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]{0,62}$");

    private final JdbcTemplate jdbcTemplate;

    public QueryExecutor(@Qualifier("sandboxJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> executeSandboxedQuery(String schemaName, String query, int timeoutSeconds) {
        return executeSandboxedScript(schemaName, query, timeoutSeconds, false).rows();
    }

    public SandboxExecutionResult executeSandboxedScript(
            String schemaName,
            String query,
            int timeoutSeconds,
            boolean rollbackAfterExecution
    ) {
        String safeSchemaName = requireSafeIdentifier(schemaName);
        return jdbcTemplate.execute((ConnectionCallback<SandboxExecutionResult>) con -> {
            boolean originalAutoCommit = con.getAutoCommit();
            try {
                con.setAutoCommit(false);

                try (Statement stmt = con.createStatement()) {
                    if (timeoutSeconds > 0) {
                        stmt.setQueryTimeout(timeoutSeconds);
                    }
                    stmt.execute("SET LOCAL search_path TO " + safeSchemaName);

                    boolean sawResultSet = false;
                    List<String> lastColumns = List.of();
                    List<Map<String, Object>> lastRows = List.of();

                    for (String statement : SqlScriptUtils.splitStatements(query)) {
                        if (stmt.execute(statement)) {
                            sawResultSet = true;
                            try (ResultSet rs = stmt.getResultSet()) {
                                QueryResult queryResult = readResultSet(rs);
                                lastColumns = queryResult.columns();
                                lastRows = queryResult.rows();
                            }
                        }
                    }

                    if (rollbackAfterExecution) {
                        con.rollback();
                    } else {
                        con.commit();
                    }

                    return new SandboxExecutionResult(sawResultSet, lastColumns, lastRows);
                }
            } catch (Exception e) {
                try {
                    con.rollback();
                } catch (Exception ignored) {
                    // Best effort rollback after sandbox execution failure.
                }
                throw new RuntimeException("Query execution failed: " + e.getMessage(), e);
            } finally {
                try {
                    con.setAutoCommit(originalAutoCommit);
                } catch (Exception ignored) {
                    // Best effort connection reset for pooled connections.
                }
            }
        });
    }

    private String requireSafeIdentifier(String identifier) {
        if (identifier == null || !IDENTIFIER_PATTERN.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Invalid sandbox schema name");
        }
        return identifier;
    }

    private QueryResult readResultSet(ResultSet rs) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();
        ResultSetMetaData metadata = rs.getMetaData();
        int columnCount = metadata.getColumnCount();
        List<String> columns = new ArrayList<>(columnCount);

        for (int i = 1; i <= columnCount; i++) {
            columns.add(metadata.getColumnLabel(i));
        }

        while (rs.next()) {
            Map<String, Object> row = new HashMap<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                row.put(columns.get(i - 1), rs.getObject(i));
            }
            results.add(row);
        }

        return new QueryResult(columns, results);
    }

    private record QueryResult(List<String> columns, List<Map<String, Object>> rows) {
    }
}
