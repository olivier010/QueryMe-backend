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
        String safeSchemaName = requireSafeIdentifier(schemaName);
        return jdbcTemplate.execute((ConnectionCallback<List<Map<String, Object>>>) con -> {
            try (Statement stmt = con.createStatement()) {
                stmt.execute("SET search_path TO " + safeSchemaName);
                if (timeoutSeconds > 0) {
                    stmt.setQueryTimeout(timeoutSeconds);
                }
                
                try (ResultSet rs = stmt.executeQuery(query)) {
                    List<Map<String, Object>> results = new ArrayList<>();
                    ResultSetMetaData md = rs.getMetaData();
                    int columns = md.getColumnCount();
                    
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>(columns);
                        for (int i = 1; i <= columns; ++i) {
                            row.put(md.getColumnLabel(i), rs.getObject(i));
                        }
                        results.add(row);
                    }
                    return results;
                } finally {
                    stmt.execute("SET search_path TO public");
                }
            } catch (Exception e) {
                try (Statement stmt = con.createStatement()) {
                     stmt.execute("SET search_path TO public");
                } catch (Exception ignored) {}
                throw new RuntimeException("Query execution failed: " + e.getMessage(), e);
            }
        });
    }

    private String requireSafeIdentifier(String identifier) {
        if (identifier == null || !IDENTIFIER_PATTERN.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Invalid sandbox schema name");
        }
        return identifier;
    }
}
