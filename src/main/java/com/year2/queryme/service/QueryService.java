package com.year2.queryme.service;

import com.year2.queryme.model.dto.SubmissionRequest;
import com.year2.queryme.model.dto.SubmissionResponse;

public interface QueryService {
    SubmissionResponse submitQuery(SubmissionRequest request);
}
