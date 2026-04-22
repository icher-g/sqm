package io.sqm.core.match;

import io.sqm.core.GroupItem;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GroupItemMatchTest {

    @Test
    void matchesSimpleGroupItem() {
        var item = GroupItem.of(col("a"));

        var result = item.matchGroupItem()
            .groupingSets(i -> "sets")
            .simple(i -> "simple")
            .orElse("none");

        assertEquals("simple", result);
    }

    @Test
    void matchesGroupingSet() {
        var item = (GroupItem.GroupingSet) GroupItem.groupingSet(GroupItem.from("a"));

        var result = item.matchGroupItem()
            .groupingSet(i -> "set")
            .orElse("none");

        assertEquals("set", result);
    }

    @Test
    void matchesGroupingSets() {
        var item = (GroupItem.GroupingSets) GroupItem.groupingSets(GroupItem.groupingSet(GroupItem.from("a")));

        var result = item.matchGroupItem()
            .groupingSets(i -> "sets")
            .orElse("none");

        assertEquals("sets", result);
    }

    @Test
    void matchesRollupAndCube() {
        var rollup = (GroupItem.Rollup) GroupItem.rollup(GroupItem.from("a"));
        var cube = (GroupItem.Cube) GroupItem.cube(GroupItem.from("a"));

        var rollupResult = rollup.matchGroupItem()
            .rollup(i -> "rollup")
            .orElse("none");
        var cubeResult = cube.matchGroupItem()
            .cube(i -> "cube")
            .orElse("none");

        assertEquals("rollup", rollupResult);
        assertEquals("cube", cubeResult);
    }

    @Test
    void otherwiseReturnsFallbackWhenNoHandlerMatches() {
        var item = (GroupItem.GroupingSet) GroupItem.groupingSet(GroupItem.from("a"));

        var result = item.matchGroupItem()
            .simple(i -> "simple")
            .otherwise(i -> "fallback");

        assertEquals("fallback", result);
    }

    @Test
    void nonMatchingHandlersAreSkipped() {
        var item = GroupItem.of(col("a"));

        var result = item.matchGroupItem()
            .groupingSet(i -> "set")
            .groupingSets(i -> "sets")
            .rollup(i -> "rollup")
            .cube(i -> "cube")
            .simple(i -> "simple")
            .orElse("none");

        assertEquals("simple", result);
    }
}
