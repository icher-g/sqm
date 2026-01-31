package io.sqm.core.match;

import io.sqm.core.GroupItem;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.group;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GroupItemMatchTest {

    @Test
    void matchesSimpleGroupItem() {
        var item = GroupItem.of(col("a"));

        var result = item.matchGroupItem()
            .simple(i -> "simple")
            .orElse("none");

        assertEquals("simple", result);
    }

    @Test
    void matchesGroupingSet() {
        var item = (GroupItem.GroupingSet) GroupItem.groupingSet(group("a"));

        var result = item.matchGroupItem()
            .groupingSet(i -> "set")
            .orElse("none");

        assertEquals("set", result);
    }

    @Test
    void matchesGroupingSets() {
        var item = (GroupItem.GroupingSets) GroupItem.groupingSets(GroupItem.groupingSet(group("a")));

        var result = item.matchGroupItem()
            .groupingSets(i -> "sets")
            .orElse("none");

        assertEquals("sets", result);
    }

    @Test
    void matchesRollupAndCube() {
        var rollup = (GroupItem.Rollup) GroupItem.rollup(group("a"));
        var cube = (GroupItem.Cube) GroupItem.cube(group("a"));

        var rollupResult = rollup.matchGroupItem()
            .rollup(i -> "rollup")
            .orElse("none");
        var cubeResult = cube.matchGroupItem()
            .cube(i -> "cube")
            .orElse("none");

        assertEquals("rollup", rollupResult);
        assertEquals("cube", cubeResult);
    }
}
