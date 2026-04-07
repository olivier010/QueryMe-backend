package com.year2.queryme.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ResultSetComparator {

    private final ObjectMapper objectMapper;

    public ResultSetComparator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean compare(List<Map<String, Object>> studentData, String expectedRowsJson, boolean orderSensitive) {
        try {
            List<Map<String, Object>> expectedData = objectMapper.readValue(expectedRowsJson, new TypeReference<>() {});
            
            if (studentData.size() != expectedData.size()) return false;
            
            List<Map<String, String>> studentNormalized = normalize(studentData);
            List<Map<String, String>> expectedNormalized = normalize(expectedData);
            
            if (orderSensitive) {
                return studentNormalized.equals(expectedNormalized);
            } else {
                List<Map<String, String>> sortedStudent = sortData(studentNormalized);
                List<Map<String, String>> sortedExpected = sortData(expectedNormalized);
                return sortedStudent.equals(sortedExpected);
            }
            
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse expected rows JSON", e);
        }
    }
    
    private List<Map<String, String>> normalize(List<Map<String, Object>> data) {
        return data.stream()
            .map(row -> row.entrySet().stream()
                .collect(Collectors.toMap(
                    e -> e.getKey().toLowerCase(), 
                    e -> normalizeValue(e.getValue())
                )))
            .collect(Collectors.toList());
    }

    private String normalizeValue(Object value) {
        if (value == null) return "null";
        if (value instanceof Number) {
            // Remove trailing zeros for decimals to match 1.0 with 1
            String str = String.valueOf(value);
            if (str.contains(".")) {
                str = str.replaceAll("0*$", "").replaceAll("\\.$", "");
            }
            return str;
        }
        return String.valueOf(value).trim();
    }
    
    private List<Map<String, String>> sortData(List<Map<String, String>> data) {
        return data.stream()
            .sorted((map1, map2) -> {
                String str1 = map1.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .sorted()
                    .collect(Collectors.joining(","));
                String str2 = map2.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .sorted()
                    .collect(Collectors.joining(","));
                return str1.compareTo(str2);
            })
            .collect(Collectors.toList());
    }
}
