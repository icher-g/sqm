package io.sqm.core.match;

import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.set;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AssignmentMatchTest {

    @Test
    void matchesAssignment() {
        var assignment = set("name", lit("alice"));

        assertEquals("name", Match.<String>assignment(assignment)
            .assignment(a -> a.column().value())
            .otherwise(ignored -> "other"));
    }
}
