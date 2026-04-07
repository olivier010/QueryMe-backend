package com.year2.queryme.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuestionRequest {

    @NotBlank(message = "Prompt is required")
    private String prompt;

    @NotBlank(message = "Reference query is required")
    private String referenceQuery;

    @NotNull(message = "Marks are required")
    private Integer marks;

    @NotNull(message = "Order index is required")
    private Integer orderIndex;

    private Boolean orderSensitive = false;
    private Boolean partialMarks = false;
}