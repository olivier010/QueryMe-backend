package com.year2.queryme.model.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class QuestionResponse {
    private UUID id;
    private UUID examId;
    private String prompt;
    private String referenceQuery;
    private Integer marks;
    private Integer orderIndex;
    private Boolean orderSensitive;
    private Boolean partialMarks;
}