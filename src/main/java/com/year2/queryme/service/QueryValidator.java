package com.year2.queryme.service;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class QueryValidator {

    private static final String[] BLOCKLIST = {
        "\\bDROP\\b", "\\bALTER\\b", "\\bCREATE\\b", "\\bTRUNCATE\\b", 
        "\\bGRANT\\b", "\\bREVOKE\\b", "\\bINSERT\\b", "\\bUPDATE\\b", 
        "\\bDELETE\\b", "\\bUPSERT\\b", "\\bMERGE\\b", "\\bEXEC\\b", 
        "\\bEXECUTE\\b"
    };

    public void validate(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be empty");
        }
        
        String upperQuery = query.toUpperCase();
        
        for (String blockedPattern : BLOCKLIST) {
            if (Pattern.compile(blockedPattern).matcher(upperQuery).find()) {
                throw new IllegalArgumentException("Query contains blocked keyword: " + blockedPattern.replace("\\b", ""));
            }
        }
    }
}
