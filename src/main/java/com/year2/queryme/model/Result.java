package com.year2.queryme.model;
import jakarta.persistence.*;
        import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "results")
@Data
public class Result {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId; // From Group G [cite: 61]

    @Column(name = "question_id", nullable = false)
    private UUID questionId; // From Group I [cite: 61]

    @Column(name = "session_id", nullable = false)
    private UUID sessionId; // Links to the student's attempt [cite: 61]

    private Integer score;
    private Integer maxScore;
    private Boolean isCorrect;
    private LocalDateTime gradedAt;
}