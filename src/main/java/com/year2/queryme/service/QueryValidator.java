package com.year2.queryme.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class QueryValidator {

    private static final String[] BLOCKLIST = {
        "\\bDROP\\b", "\\bALTER\\b", "\\bCREATE\\b", "\\bTRUNCATE\\b", 
        "\\bGRANT\\b", "\\bREVOKE\\b", "\\bINSERT\\b", "\\bUPDATE\\b", 
        "\\bDELETE\\b", "\\bUPSERT\\b", "\\bMERGE\\b", "\\bEXEC\\b", 
        "\\bEXECUTE\\b", "\\bCALL\\b", "\\bDO\\b", "\\bCOPY\\b", "\\bVACUUM\\b",
        "\\bANALYZE\\b", "\\bSET\\b", "\\bRESET\\b", "\\bSHOW\\b"
    };

    private static final Pattern LEADING_QUERY_PATTERN = Pattern.compile("^(SELECT|WITH)\\b", Pattern.CASE_INSENSITIVE);

    public void validate(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be empty");
        }

        String trimmedQuery = query.trim();
        if (!LEADING_QUERY_PATTERN.matcher(trimmedQuery).find()) {
            throw new IllegalArgumentException("Only SELECT queries are allowed");
        }

        int semicolonCount = (int) trimmedQuery.chars().filter(ch -> ch == ';').count();
        if (semicolonCount > 1 || (semicolonCount == 1 && !trimmedQuery.endsWith(";"))) {
            throw new IllegalArgumentException("Multiple SQL statements are not allowed");
        }

        if (trimmedQuery.contains("--") || trimmedQuery.contains("/*") || trimmedQuery.contains("*/")) {
            throw new IllegalArgumentException("SQL comments are not allowed in submissions");
        }
        
        String upperQuery = trimmedQuery.toUpperCase();
        
        for (String blockedPattern : BLOCKLIST) {
            if (Pattern.compile(blockedPattern).matcher(upperQuery).find()) {
                throw new IllegalArgumentException("Query contains blocked keyword: " + blockedPattern.replace("\\b", ""));
            }
        }
    }
}
