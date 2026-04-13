package com.year2.queryme.service;

import com.year2.queryme.model.Exam;
import com.year2.queryme.model.dto.ExamResponse;
import com.year2.queryme.model.enums.ExamStatus;
import com.year2.queryme.repository.CourseEnrollmentRepository;
import com.year2.queryme.repository.ExamRepository;
import com.year2.queryme.repository.QuestionRepository;
import com.year2.queryme.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamServiceImplTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Mock
    private QuestionRepository questionRepository;

    private ExamServiceImpl examService;

    @BeforeEach
    void setUp() {
        examService = new ExamServiceImpl(
                examRepository,
                currentUserService,
                studentRepository,
                courseEnrollmentRepository,
                questionRepository
        );
    }

    @Test
    void getPublishedExamsIncludesQuestionCounts() {
        String examId = UUID.randomUUID().toString();
        Exam exam = Exam.builder()
                .id(examId)
                .courseId("1")
                .title("Retail Orders SQL Practice")
                .status(ExamStatus.PUBLISHED)
                .build();

        when(examRepository.findByStatus(ExamStatus.PUBLISHED)).thenReturn(List.of(exam));
        when(questionRepository.countByExamId(UUID.fromString(examId))).thenReturn(7L);

        List<ExamResponse> responses = examService.getPublishedExams();

        assertEquals(1, responses.size());
        assertEquals(7, responses.get(0).getQuestionCount());
        assertEquals(7, responses.get(0).getQuestionsCount());
    }
}
