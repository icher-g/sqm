package io.sqm.core.match;

import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.set;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AssignmentMatchTest {

    @Test
    void matchesAssignment() {
        var assignment = set("u", "name", lit("alice"));

        assertEquals("u.name", Match.<String>assignment(assignment)
            .assignment(a -> String.join(".", a.column().values()))
            .otherwise(ignored -> "other"));
    }
}