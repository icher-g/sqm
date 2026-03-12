package io.sqm.transpile;

import io.sqm.dsl.Dsl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranspileResultTest {

    @Test
    void successDependsOnStatusAndCopiesCollections() {
        var steps = new ArrayList<TranspileStep>();
        var problems = new ArrayList<TranspileProblem>();
        var warnings = new ArrayList<TranspileWarning>();
        var result = new TranspileResult(
            TranspileStatus.SUCCESS_WITH_WARNINGS,
            Optional.of(Dsl.select(Dsl.lit(1)).build()),
            Optional.of(Dsl.select(Dsl.lit(1)).build()),
            Optional.of("SELECT 1"),
            steps,
            problems,
            warnings
        );

        assertTrue(result.success());
        assertNotSame(steps, result.steps());
        assertNotSame(problems, result.problems());
        assertNotSame(warnings, result.warnings());
        assertFalse(new TranspileResult(
            TranspileStatus.UNSUPPORTED,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            List.of(),
            List.of(),
            List.of()
        ).success());
    }

    @Test
    void rejectsNullComponents() {
        assertThrows(NullPointerException.class, () -> new TranspileResult(
            null,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            List.of(),
            List.of(),
            List.of()
        ));
    }
}
