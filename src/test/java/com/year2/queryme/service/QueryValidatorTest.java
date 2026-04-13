package com.year2.queryme.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryValidatorTest {

    private final QueryValidator queryValidator = new QueryValidator();

    @Test
    void allowsSandboxScopedMultiStatementScript() {
        assertDoesNotThrow(() -> queryValidator.validate(
                "CREATE TABLE work_log(id INT); INSERT INTO work_log VALUES (1); SELECT * FROM work_log;",
                "exam_12345678_student_87654321",
                false
        ));
    }

    @Test
    void allowsUpdateStatementsThatUseSetClauses() {
        assertDoesNotThrow(() -> queryValidator.validate(
                "UPDATE salary SET amount = amount + 1 WHERE id = 7",
                "exam_12345678_student_87654321",
                false
        ));
    }

    @Test
    void rejectsCrossSchemaReads() {
        assertThrows(IllegalArgumentException.class, () -> queryValidator.validate(
                "SELECT * FROM public.salary",
                "exam_12345678_student_87654321",
                false
        ));
    }

    @Test
    void rejectsCrossSandboxWrites() {
        assertThrows(IllegalArgumentException.class, () -> queryValidator.validate(
                "INSERT INTO exam_deadbeef_student_feedface.salary VALUES (1)",
                "exam_12345678_student_87654321",
                false
        ));
    }

    @Test
    void requiresReferenceScriptsToEndWithARowReturningStatement() {
        assertThrows(IllegalArgumentException.class, () -> queryValidator.validate(
                "UPDATE salary SET amount = amount + 1",
                "exam_12345678_student_87654321",
                true
        ));
    }
}
