package com.year2.queryme.controller;

import com.year2.queryme.model.Result;
import com.year2.queryme.service.ResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/results")
public class ResultController {

    @Autowired
    private ResultService resultService;

    /**
     * ENDPOINT FOR STUDENT PORTAL (Group B)
     * Retrieves results for a specific session, subject to visibility logic.
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<Result>> getStudentResults(@PathVariable UUID sessionId) {
        // Logic inside the service will check Group A's visibility settings
        List<Result> results = resultService.getResultsForStudent(sessionId);
        return ResponseEntity.ok(results);
    }

    /**
     * ENDPOINT FOR TEACHER PORTAL (Group H)
     * Retrieves the full results dashboard for a specific exam.
     * Restricted to users with TEACHER role.
     */
    @GetMapping("/exam/{examId}/dashboard")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<Result>> getTeacherDashboard(@PathVariable UUID examId) {
        /* * LOGIC FROM GROUP J (Auth):
         * The @PreAuthorize annotation uses the SecurityConfig and JWT filter
         * managed by Group J to ensure only Teachers access this data.
         */
        List<Result> dashboardData = resultService.getResultsForTeacher(examId);
        return ResponseEntity.ok(dashboardData);
    }
}