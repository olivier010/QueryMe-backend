package com.year2.queryme.repository;

import com.year2.queryme.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {
    List<Question> findByExamIdOrderByOrderIndexAsc(UUID examId);
    long countByExamId(UUID examId);
}
