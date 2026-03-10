package io.sqm.parser.ansi;

import io.sqm.core.Assignment;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignmentParserTest {

    @Test
    void parsesSimpleAssignment() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(Assignment.class, "name = 'alice'");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(java.util.List.of("name"), result.value().column().values());
    }

    @Test
    void rejectsNonEqualsOperatorInAssignment() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(Assignment.class, "name <> 1");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Expected = in assignment"));
    }

    @Test
    void rejectsAssignmentWithoutRightHandExpression() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(Assignment.class, "name =");

        assertTrue(result.isError());
    }

    @Test
    void exposesAssignmentTargetType() {
        assertEquals(Assignment.class, new AssignmentParser().targetType());
    }
}
