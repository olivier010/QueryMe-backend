package com.year2.queryme.model.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class SubmissionResponse {
    private UUID submissionId;
    private Boolean isCorrect;
    private Integer score;
    private String executionError;
}
