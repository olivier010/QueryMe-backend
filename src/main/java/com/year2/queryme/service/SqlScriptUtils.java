package com.year2.queryme.service;

import java.util.ArrayList;
import java.util.List;

final class SqlScriptUtils {

    private SqlScriptUtils() {
    }

    static List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);

            if (ch == '\'' && !inDoubleQuote) {
                current.append(ch);
                if (inSingleQuote && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    current.append(sql.charAt(++i));
                    continue;
                }
                inSingleQuote = !inSingleQuote;
                continue;
            }

            if (ch == '"' && !inSingleQuote) {
                current.append(ch);
                if (inDoubleQuote && i + 1 < sql.length() && sql.charAt(i + 1) == '"') {
                    current.append(sql.charAt(++i));
                    continue;
                }
                inDoubleQuote = !inDoubleQuote;
                continue;
            }

            if (ch == ';' && !inSingleQuote && !inDoubleQuote) {
                addStatement(statements, current);
                current.setLength(0);
                continue;
            }

            current.append(ch);
        }

        if (inSingleQuote || inDoubleQuote) {
            throw new IllegalArgumentException("Unterminated quoted literal in SQL script");
        }

        addStatement(statements, current);
        return statements;
    }

    static String maskSingleQuotedLiterals(String sql) {
        StringBuilder masked = new StringBuilder(sql.length());
        boolean inSingleQuote = false;

        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);

            if (ch == '\'') {
                masked.append(ch);
                if (inSingleQuote && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    masked.append(' ');
                    i++;
                    continue;
                }
                inSingleQuote = !inSingleQuote;
                continue;
            }

            masked.append(inSingleQuote ? ' ' : ch);
        }

        return masked.toString();
    }

    private static void addStatement(List<String> statements, StringBuilder current) {
        String statement = current.toString().trim();
        if (!statement.isBlank()) {
            statements.add(statement);
        }
    }
}
