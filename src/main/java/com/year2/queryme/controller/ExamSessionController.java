package com.year2.queryme.controller;

import com.year2.queryme.model.dto.*;
import com.year2.queryme.service.ExamSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class ExamSessionController {

    private final ExamSessionService sessionService;

    @PostMapping("/start")
    public ResponseEntity<ExamSessionResponse> start(
            @RequestBody StartSessionRequest request) {
        return ResponseEntity.ok(sessionService.startSession(request));
    }

    @PatchMapping("/{sessionId}/submit")
    public ResponseEntity<ExamSessionResponse> submit(
            @PathVariable String sessionId) {
        return ResponseEntity.ok(sessionService.submitSession(sessionId));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<ExamSessionResponse> getById(
            @PathVariable String sessionId) {
        return ResponseEntity.ok(sessionService.getSessionById(sessionId));
    }

    @GetMapping("/exam/{examId}")
    public ResponseEntity<List<ExamSessionResponse>> getByExam(
            @PathVariable String examId) {
        return ResponseEntity.ok(sessionService.getSessionsByExam(examId));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<ExamSessionResponse>> getByStudent(
            @PathVariable String studentId) {
        return ResponseEntity.ok(sessionService.getSessionsByStudent(studentId));
    }
}