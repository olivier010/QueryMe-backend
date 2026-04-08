package com.year2.queryme.controller;

import com.year2.queryme.model.dto.SubmissionRequest;
import com.year2.queryme.model.dto.SubmissionResponse;
import com.year2.queryme.service.QueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    @PostMapping("/submit")
    public ResponseEntity<SubmissionResponse> submitQuery(@RequestBody SubmissionRequest request) {
        return ResponseEntity.ok(queryService.submitQuery(request));
    }
}
