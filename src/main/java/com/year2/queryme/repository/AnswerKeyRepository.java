package com.year2.queryme.repository;

import com.year2.queryme.model.AnswerKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnswerKeyRepository extends JpaRepository<AnswerKey, UUID> {
    Optional<AnswerKey> findByQuestionId(UUID questionId);
}