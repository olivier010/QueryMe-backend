package com.year2.queryme.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.year2.queryme.model.AnswerKey;
import com.year2.queryme.model.Exam;
import com.year2.queryme.model.Question;
import com.year2.queryme.model.dto.QuestionRequest;
import com.year2.queryme.repository.AnswerKeyRepository;
import com.year2.queryme.repository.CourseEnrollmentRepository;
import com.year2.queryme.repository.ExamRepository;
import com.year2.queryme.repository.QuestionRepository;
import com.year2.queryme.repository.StudentRepository;
import com.year2.queryme.sandbox.service.SandboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceImplTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AnswerKeyRepository answerKeyRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private SandboxService sandboxService;

    @Mock
    private QueryExecutor queryExecutor;

    @Mock
    private QueryValidator queryValidator;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;

    private QuestionServiceImpl questionService;

    @BeforeEach
    void setUp() {
        questionService = new QuestionServiceImpl(
                questionRepository,
                answerKeyRepository,
                examRepository,
                sandboxService,
                queryExecutor,
                queryValidator,
                new ObjectMapper(),
                currentUserService,
                studentRepository,
                courseEnrollmentRepository
        );
    }

    @Test
    void createQuestionKeepsPreviewSandboxOnSuccess() {
        UUID examId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID teacherUserId = UUID.randomUUID();
        QuestionRequest request = buildRequest();
        Exam exam = Exam.builder().id(examId.toString()).seedSql("create table orders(id int);").build();

        when(currentUserService.requireCurrentUserId()).thenReturn(teacherUserId);
        when(examRepository.findById(examId.toString())).thenReturn(Optional.of(exam));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            Question question = invocation.getArgument(0);
            question.setId(questionId);
            return question;
        });
        when(sandboxService.provisionSandbox(examId, teacherUserId, exam.getSeedSql())).thenReturn("preview_schema");
        doNothing().when(queryValidator).validate(request.getReferenceQuery(), "preview_schema", true);
        when(queryExecutor.executeSandboxedScript("preview_schema", request.getReferenceQuery(), 5, true))
                .thenReturn(new SandboxExecutionResult(true, List.of("total_orders"), List.of(Map.of("total_orders", 42))));
        when(answerKeyRepository.findByQuestionId(questionId)).thenReturn(Optional.empty());

        questionService.createQuestion(examId, request);

        verify(sandboxService, never()).teardownSandbox(any(UUID.class), any(UUID.class));
        verify(answerKeyRepository).save(any(AnswerKey.class));
    }

    @Test
    void createQuestionCleansUpPreviewSandboxOnFailure() {
        UUID examId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID teacherUserId = UUID.randomUUID();
        QuestionRequest request = buildRequest();
        Exam exam = Exam.builder().id(examId.toString()).seedSql("create table orders(id int);").build();

        when(currentUserService.requireCurrentUserId()).thenReturn(teacherUserId);
        when(examRepository.findById(examId.toString())).thenReturn(Optional.of(exam));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            Question question = invocation.getArgument(0);
            question.setId(questionId);
            return question;
        });
        when(sandboxService.provisionSandbox(examId, teacherUserId, exam.getSeedSql())).thenReturn("preview_schema");
        doNothing().when(queryValidator).validate(request.getReferenceQuery(), "preview_schema", true);
        when(queryExecutor.executeSandboxedScript("preview_schema", request.getReferenceQuery(), 5, true))
                .thenThrow(new RuntimeException("reference query failed"));

        assertThrows(RuntimeException.class, () -> questionService.createQuestion(examId, request));

        verify(sandboxService).teardownSandbox(eq(examId), eq(teacherUserId));
    }

    private QuestionRequest buildRequest() {
        QuestionRequest request = new QuestionRequest();
        request.setPrompt("How many orders are there?");
        request.setReferenceQuery("SELECT COUNT(*) AS total_orders FROM orders");
        request.setMarks(5);
        request.setOrderIndex(1);
        request.setOrderSensitive(false);
        request.setPartialMarks(false);
        return request;
    }
}
