package com.year2.queryme.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "answer_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    // Hibernate 6 will automatically map this String to PostgreSQL JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "expected_columns", nullable = false, columnDefinition = "jsonb")
    private String expectedColumns;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "expected_rows", nullable = false, columnDefinition = "jsonb")
    private String expectedRows;

    @CreationTimestamp
    @Column(name = "generated_at", updatable = false)
    private LocalDateTime generatedAt;
}