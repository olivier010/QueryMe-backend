package com.year2.queryme.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exam_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamSession {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private UUID examId;
    
    @Column(nullable = false)
    private UUID studentId;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "sandbox_schema")
    private String sandboxSchema;
    
    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
