package io.sqm.transpile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TranspileProblemTest {

    @Test
    void storesProblemFieldsIncludingOptionalSourceOffset() {
        var problem = new TranspileProblem("PARSE_ERROR", "Expected FROM", TranspileStage.PARSE, 7, 1, 8);

        assertEquals("PARSE_ERROR", problem.code());
        assertEquals("Expected FROM", problem.message());
        assertEquals(TranspileStage.PARSE, problem.stage());
        assertEquals(7, problem.sourceOffset());
        assertEquals(1, problem.line());
        assertEquals(8, problem.column());
        assertNull(problem.statementIndex());
    }

    @Test
    void allowsMissingSourceOffset() {
        var problem = new TranspileProblem("RENDER_FAILED", "Unsupported", TranspileStage.RENDER);

        assertNull(problem.sourceOffset());
        assertNull(problem.line());
        assertNull(problem.column());
        assertNull(problem.statementIndex());
    }

    @Test
    void storesProblemWithOnlySourceOffset() {
        var problem = new TranspileProblem("PARSE_ERROR", "Expected FROM", TranspileStage.PARSE, 7);

        assertEquals(7, problem.sourceOffset());
        assertNull(problem.line());
        assertNull(problem.column());
        assertNull(problem.statementIndex());
    }

    @Test
    void withStatementIndexCopiesProblemContext() {
        var problem = new TranspileProblem("PARSE_ERROR", "Expected FROM", TranspileStage.PARSE, 7, 1, 8);

        var indexed = problem.withStatementIndex(2);

        assertEquals(problem.code(), indexed.code());
        assertEquals(problem.message(), indexed.message());
        assertEquals(problem.stage(), indexed.stage());
        assertEquals(problem.sourceOffset(), indexed.sourceOffset());
        assertEquals(problem.line(), indexed.line());
        assertEquals(problem.column(), indexed.column());
        assertEquals(2, indexed.statementIndex());
    }

    @Test
    void withStatementIndexRejectsNonPositiveIndex() {
        var problem = new TranspileProblem("PARSE_ERROR", "Expected FROM", TranspileStage.PARSE);

        assertThrows(IllegalArgumentException.class, () -> problem.withStatementIndex(0));
    }
}
