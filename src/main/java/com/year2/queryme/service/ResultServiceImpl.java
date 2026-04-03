package com.year2.queryme.service;

import com.year2.queryme.model.Result;
import com.year2.queryme.repository.ResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ResultServiceImpl implements ResultService {

    @Autowired
    private ResultRepository resultRepository;

    // TODO: GROUP A (Exam Module) - Inject ExamService here
    // You need this to check visibility_mode and exam end times.

    @Override
    public List<Result> getResultsForStudent(UUID sessionId) {
        /* * LOGIC FROM GROUP A (Exam):
         * 1. Call examService.getSessionById(sessionId) to get the exam_id.
         * 2. Call examService.getExamSettings(examId) to retrieve 'visibility_mode'.
         */

        // Mocking the check for now:
        String visibilityMode = "END_OF_EXAM"; // This should come from Group A [cite: 61]
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1); // This should come from Group A [cite: 60]

        // Visibility Gatekeeping Logic:
        if ("NEVER".equalsIgnoreCase(visibilityMode)) {
            return Collections.emptyList();
        }

        if ("END_OF_EXAM".equalsIgnoreCase(visibilityMode)) {
            // Logic Check: Is the exam still active?
            if (LocalDateTime.now().isBefore(expiresAt)) {
                return Collections.emptyList(); // Block access until timer expires [cite: 30, 62]
            }
        }

        return resultRepository.findBySessionId(sessionId);
    }

    @Override
    public void processNewSubmission(UUID submissionId) {
        /* * LOGIC FROM GROUP G (Query Engine):
         * 1. Call queryService.getSubmissionById(submissionId).
         * 2. Group G provides the 'mark', 'max_mark', and 'is_correct' status
         * calculated by their result-set comparator[cite: 45, 61].
         */

        // Once data is received from Group G, map it to your 'results' table:
        Result result = new Result();
        result.setSubmissionId(submissionId);
        result.setGradedAt(LocalDateTime.now());

        // These values are the output of Group G's grading logic[cite: 61]:
        // result.setScore(submission.getMark());
        // result.setIsCorrect(submission.getIsCorrect());

        resultRepository.save(result);
    }

    @Override
    public List<Result> getResultsForTeacher(UUID examId) {
        /* * LOGIC FROM GROUP F (User/Student):
         * When returning this list to the Teacher Dashboard (Group H),
         * you may need to call userService.getStudentDetails() to show
         * real names instead of just UUIDs[cite: 61].
         */
        return resultRepository.findAllByExamId(examId);
    }

    @Override
    public Result saveQueryResult(UUID submissionId, Integer score, Boolean isCorrect) {
        return null;
    }
}