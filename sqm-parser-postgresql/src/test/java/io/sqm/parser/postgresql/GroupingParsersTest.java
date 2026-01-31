package io.sqm.parser.postgresql;

import io.sqm.core.GroupItem;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PostgreSQL Grouping Parsers Tests")
class GroupingParsersTest {

    private final ParseContext ctx = ParseContext.of(new PostgresSpecs());

    @Test
    @DisplayName("Parse GROUPING SETS with empty and non-empty sets")
    void parsesGroupingSets() {
        var result = ctx.parse(GroupItem.GroupingSets.class, "GROUPING SETS ((), (a))");

        assertTrue(result.ok(), () -> "expected ok, got error: " + result.errorMessage());
        assertEquals(2, result.value().sets().size());
        var emptySet = (GroupItem.GroupingSet) result.value().sets().getFirst();
        assertTrue(emptySet.items().isEmpty());
    }

    @Test
    @DisplayName("Parse empty GROUPING SETS list")
    void parsesEmptyGroupingSets() {
        var result = ctx.parse(GroupItem.GroupingSets.class, "GROUPING SETS ()");

        assertTrue(result.ok(), () -> "expected ok, got error: " + result.errorMessage());
        assertTrue(result.value().sets().isEmpty());
    }

    @Test
    @DisplayName("Parse grouping set")
    void parsesGroupingSet() {
        var result = ctx.parse(GroupItem.GroupingSet.class, "(a)");

        assertTrue(result.ok(), () -> "expected ok, got error: " + result.errorMessage());
        assertEquals(1, result.value().items().size());
    }

    @Test
    @DisplayName("Parse empty grouping set")
    void parsesEmptyGroupingSet() {
        var result = ctx.parse(GroupItem.GroupingSet.class, "()");

        assertTrue(result.ok(), () -> "expected ok, got error: " + result.errorMessage());
        assertTrue(result.value().items().isEmpty());
    }

    @Test
    @DisplayName("Parse CUBE")
    void parsesCube() {
        var result = ctx.parse(GroupItem.Cube.class, "CUBE (a)");

        assertTrue(result.ok(), () -> "expected ok, got error: " + result.errorMessage());
        assertEquals(1, result.value().items().size());
    }

    @Test
    @DisplayName("Reject empty CUBE")
    void rejectsEmptyCube() {
        var result = ctx.parse(GroupItem.Cube.class, "CUBE ()");
        assertFalse(result.ok());
    }
}
