package com.year2.queryme.service;

import com.year2.queryme.model.Result;
import java.util.List;
import java.util.UUID;

public interface ResultService {
    List<Result> getResultsForStudent(UUID sessionId);
    void processNewSubmission(UUID submissionId);
    List<Result> getResultsForTeacher(UUID examId);
    Result saveQueryResult(UUID submissionId, Integer score, Boolean isCorrect);
}
