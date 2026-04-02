package com.year2.queryme.repository;

import com.year2.queryme.model.ExamSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamSessionRepository extends JpaRepository<ExamSession, String> {
    Optional<ExamSession> findByExamIdAndStudentId(UUID examId, UUID studentId);
}
