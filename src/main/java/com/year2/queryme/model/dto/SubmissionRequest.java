package com.year2.queryme.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class SubmissionRequest {
    private UUID sessionId;
    @NotNull
    private UUID examId;
    @NotNull
    private UUID questionId;
    @NotNull
    private UUID studentId;
    @NotBlank
    private String query;
}
