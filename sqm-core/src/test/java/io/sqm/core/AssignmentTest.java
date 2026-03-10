package io.sqm.core;

import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.lit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssignmentTest {

    @Test
    void createsAssignmentWithQualifiedTarget() {
        var assignment = Assignment.of(QualifiedName.of("u", "name"), lit("alice"));

        assertEquals(java.util.List.of("u", "name"), assignment.column().values());
        assertEquals("alice", ((LiteralExpr) assignment.value()).value());
    }

    @Test
    void wrapsIdentifierTargetAsSinglePartQualifiedName() {
        var assignment = Assignment.of(Identifier.of("name"), lit("alice"));

        assertEquals(java.util.List.of("name"), assignment.column().values());
    }

    @Test
    void validatesRequiredMembers() {
        assertThrows(NullPointerException.class, () -> Assignment.of((QualifiedName) null, lit("alice")));
        assertThrows(NullPointerException.class, () -> Assignment.of(QualifiedName.of("name"), null));
    }
}