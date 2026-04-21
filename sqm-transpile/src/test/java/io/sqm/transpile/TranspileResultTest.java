package io.sqm.transpile;

import io.sqm.dsl.Dsl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranspileResultTest {

    @Test
    void successDependsOnStatusAndCopiesCollections() {
        var steps = new ArrayList<TranspileStep>();
        var params = new ArrayList<>();
        params.add(1L);
        var problems = new ArrayList<TranspileProblem>();
        var warnings = new ArrayList<TranspileWarning>();
        var result = new TranspileResult(
            TranspileStatus.SUCCESS_WITH_WARNINGS,
            Optional.of(Dsl.select(Dsl.lit(1)).build()),
            Optional.of(Dsl.select(Dsl.lit(1)).build()),
            Optional.of("SELECT 1"),
            params,
            steps,
            problems,
            warnings
        );

        assertTrue(result.success());
        assertEquals(List.of(1L), result.params());
        assertNotSame(params, result.params());
        assertNotSame(steps, result.steps());
        assertNotSame(problems, result.problems());
        assertNotSame(warnings, result.warnings());
        assertFalse(new TranspileResult(
            TranspileStatus.UNSUPPORTED,
            null,
            null,
            null,
            List.of(),
            List.of(),
            List.of()
        ).success());
    }

    @Test
    void rejectsNullComponents() {
        assertThrows(NullPointerException.class, () -> new TranspileResult(
            null,
            null,
            null,
            null,
            List.of(),
            List.of(),
            List.of()
        ));
    }
}
