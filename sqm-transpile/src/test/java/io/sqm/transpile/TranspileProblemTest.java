package io.sqm.transpile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
    }

    @Test
    void allowsMissingSourceOffset() {
        var problem = new TranspileProblem("RENDER_FAILED", "Unsupported", TranspileStage.RENDER);

        assertNull(problem.sourceOffset());
        assertNull(problem.line());
        assertNull(problem.column());
    }
}
