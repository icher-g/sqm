package io.sqm.parser.ansi;

import io.sqm.core.GroupItem;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisplayName("ANSI Grouping Extensions Parser Tests")
class GroupingExtensionsParserTest {

    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());

    @Test
    @DisplayName("GROUPING SETS is rejected")
    void groupingSetsRejected() {
        var result = ctx.parse(GroupItem.GroupingSets.class, "GROUPING SETS (a)");
        assertFalse(result.ok());
    }

    @Test
    @DisplayName("Grouping set is rejected")
    void groupingSetRejected() {
        var result = ctx.parse(GroupItem.GroupingSet.class, "(a)");
        assertFalse(result.ok());
    }

    @Test
    @DisplayName("ROLLUP is rejected")
    void rollupRejected() {
        var result = ctx.parse(GroupItem.Rollup.class, "ROLLUP (a)");
        assertFalse(result.ok());
    }

    @Test
    @DisplayName("CUBE is rejected")
    void cubeRejected() {
        var result = ctx.parse(GroupItem.Cube.class, "CUBE (a)");
        assertFalse(result.ok());
    }

    @Test
    @DisplayName("GROUPING SETS with multiple sets is rejected")
    void groupingSetsMultipleSetsRejected() {
        var result = ctx.parse(GroupItem.GroupingSets.class, "GROUPING SETS ((a, b), (c))");
        assertFalse(result.ok());
    }

    @Test
    @DisplayName("GROUPING SETS with empty set is rejected")
    void groupingSetsEmptySetRejected() {
        var result = ctx.parse(GroupItem.GroupingSets.class, "GROUPING SETS (())");
        assertFalse(result.ok());
    }

    @Test
    @DisplayName("ROLLUP with multiple items is rejected")
    void rollupMultipleItemsRejected() {
        var result = ctx.parse(GroupItem.Rollup.class, "ROLLUP (a, b, c)");
        assertFalse(result.ok());
    }

    @Test
    @DisplayName("CUBE with multiple items is rejected")
    void cubeMultipleItemsRejected() {
        var result = ctx.parse(GroupItem.Cube.class, "CUBE (a, b, c)");
        assertFalse(result.ok());
    }

    @Test
    @DisplayName("Target types are correct")
    void targetTypesAreCorrect() {
        var groupingSetsParser = new GroupingSetsParser();
        assertEquals(GroupItem.GroupingSets.class, groupingSetsParser.targetType());
    }
}
