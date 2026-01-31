package io.sqm.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.dsl.Dsl.group;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GroupItem Tests")
class GroupItemTest {

    @Test
    @DisplayName("Grouping set copies items")
    void groupingSetCopiesItems() {
        var items = new ArrayList<>(List.of(group("a"), group("b")));
        var set = (GroupItem.GroupingSet) GroupItem.groupingSet(items);

        items.add(group("c"));
        assertEquals(2, set.items().size());
    }

    @Test
    @DisplayName("Grouping sets copies elements")
    void groupingSetsCopiesElements() {
        var elements = new ArrayList<>(List.of(
            GroupItem.groupingSet(group("a")),
            GroupItem.groupingSet()
        ));
        var sets = (GroupItem.GroupingSets) GroupItem.groupingSets(elements);

        elements.add(GroupItem.groupingSet(group("b")));
        assertEquals(2, sets.sets().size());
    }

    @Test
    @DisplayName("Rollup and cube keep items")
    void rollupAndCubeKeepItems() {
        var rollup = (GroupItem.Rollup) GroupItem.rollup(group("a"), group("b"));
        var cube = (GroupItem.Cube) GroupItem.cube(group("a"), group("b"));

        assertEquals(2, rollup.items().size());
        assertEquals(2, cube.items().size());
    }

    @Test
    @DisplayName("Grouping nodes dispatch accept to dedicated visitor methods")
    void groupingAcceptDispatch() {
        var simple = GroupItem.of(1);
        var groupingSet = (GroupItem.GroupingSet) GroupItem.groupingSet(group("a"));
        var groupingSets = (GroupItem.GroupingSets) GroupItem.groupingSets(groupingSet);
        var rollup = (GroupItem.Rollup) GroupItem.rollup(group("a"));
        var cube = (GroupItem.Cube) GroupItem.cube(group("a"));

        var visitor = new io.sqm.core.walk.RecursiveNodeVisitor<String>() {
            @Override
            protected String defaultResult() {
                return null;
            }

            @Override
            public String visitSimpleGroupItem(GroupItem.SimpleGroupItem i) {
                return "Simple";
            }

            @Override
            public String visitGroupingSet(GroupItem.GroupingSet i) {
                return "GroupingSet";
            }

            @Override
            public String visitGroupingSets(GroupItem.GroupingSets i) {
                return "GroupingSets";
            }

            @Override
            public String visitRollup(GroupItem.Rollup i) {
                return "Rollup";
            }

            @Override
            public String visitCube(GroupItem.Cube i) {
                return "Cube";
            }
        };

        assertEquals("Simple", simple.accept(visitor));
        assertEquals("GroupingSet", groupingSet.accept(visitor));
        assertEquals("GroupingSets", groupingSets.accept(visitor));
        assertEquals("Rollup", rollup.accept(visitor));
        assertEquals("Cube", cube.accept(visitor));
    }
}
