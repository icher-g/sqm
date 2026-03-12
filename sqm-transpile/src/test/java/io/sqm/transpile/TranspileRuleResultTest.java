package io.sqm.transpile;

import io.sqm.dsl.Dsl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TranspileRuleResultTest {

    @Test
    void factoriesProduceExpectedShapesAndCopyCollections() {
        var statement = Dsl.select(Dsl.lit(1)).build();
        var warnings = new ArrayList<TranspileWarning>();
        var problems = new ArrayList<TranspileProblem>();
        var direct = new TranspileRuleResult(
            statement,
            true,
            RewriteFidelity.APPROXIMATE,
            warnings,
            problems,
            "Changed"
        );

        assertNotSame(warnings, direct.warnings());
        assertNotSame(problems, direct.problems());

        var unchanged = TranspileRuleResult.unchanged(statement, "No-op");
        assertEquals(RewriteFidelity.EXACT, unchanged.fidelity());
        assertFalse(unchanged.changed());

        var rewritten = TranspileRuleResult.rewritten(statement, RewriteFidelity.EXACT, "Rewrite");
        assertTrue(rewritten.changed());

        var unsupported = TranspileRuleResult.unsupported(statement, "UNSUPPORTED", "No mapping");
        assertEquals(RewriteFidelity.UNSUPPORTED, unsupported.fidelity());
        assertEquals("UNSUPPORTED", unsupported.problems().getFirst().code());
    }

    @Test
    void rejectsNullRequiredComponents() {
        var statement = Dsl.select(Dsl.lit(1)).build();
        assertThrows(NullPointerException.class, () -> new TranspileRuleResult(
            statement,
            false,
            null,
            List.of(),
            List.of(),
            "desc"
        ));
    }
}
