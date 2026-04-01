package com.year2.queryme.service;
import com.year2.queryme.model.dto.*;
import java.util.List;


public interface ExamSessionService {
    ExamSessionResponse startSession(StartSessionRequest request);
    ExamSessionResponse submitSession(String sessionId);
    ExamSessionResponse getSessionById(String sessionId);
    List<ExamSessionResponse> getSessionsByExam(String examId);
    List<ExamSessionResponse> getSessionsByStudent(String studentId);
}