package io.sqm.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.dsl.Dsl.col;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GroupItem Tests")
class GroupItemTest {

    @Test
    @DisplayName("Grouping set copies items")
    void groupingSetCopiesItems() {
        var items = new ArrayList<>(List.of(GroupItem.from("a"), GroupItem.from("b")));
        var set = (GroupItem.GroupingSet) GroupItem.groupingSet(items);

        items.add(GroupItem.from("c"));
        assertEquals(2, set.items().size());
    }

    @Test
    @DisplayName("Grouping sets copies elements")
    void groupingSetsCopiesElements() {
        var elements = new ArrayList<>(List.of(
            GroupItem.groupingSet(GroupItem.from("a")),
            GroupItem.groupingSet()
        ));
        var sets = (GroupItem.GroupingSets) GroupItem.groupingSets(elements);

        elements.add(GroupItem.groupingSet(GroupItem.from("b")));
        assertEquals(2, sets.sets().size());
    }

    @Test
    @DisplayName("Rollup and cube keep items")
    void rollupAndCubeKeepItems() {
        var rollup = (GroupItem.Rollup) GroupItem.rollup(GroupItem.from("a"), GroupItem.from("b"));
        var cube = (GroupItem.Cube) GroupItem.cube(GroupItem.from("a"), GroupItem.from("b"));

        assertEquals(2, rollup.items().size());
        assertEquals(2, cube.items().size());
    }

    @Test
    @DisplayName("Grouping nodes dispatch accept to dedicated visitor methods")
    void groupingAcceptDispatch() {
        var simple = GroupItem.of(1);
        var groupingSet = (GroupItem.GroupingSet) GroupItem.groupingSet(GroupItem.from("a"));
        var groupingSets = (GroupItem.GroupingSets) GroupItem.groupingSets(groupingSet);
        var rollup = (GroupItem.Rollup) GroupItem.rollup(GroupItem.from("a"));
        var cube = (GroupItem.Cube) GroupItem.cube(GroupItem.from("a"));

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

    @Test
    @DisplayName("Simple group items expose ordinal and expression")
    void simpleGroupItemState() {
        var ordinalItem = GroupItem.of(2);
        assertTrue(ordinalItem.isOrdinal());
        assertEquals(2, ordinalItem.ordinal());
        assertNull(ordinalItem.expr());

        var exprItem = GroupItem.of(col("a"));
        assertFalse(exprItem.isOrdinal());
        assertNotNull(exprItem.expr());
        assertNull(exprItem.ordinal());
    }

    @Test
    @DisplayName("GroupItem.from converts supported objects and rejects unsupported inputs")
    void fromConvertsSupportedInputsAndRejectsUnsupportedInputs() {
        var existing = GroupItem.of(col("existing"));
        var expr = col("expr");

        assertSame(existing, GroupItem.from(existing));
        assertEquals("name", assertInstanceOf(ColumnExpr.class, ((GroupItem.SimpleGroupItem) GroupItem.from("name")).expr()).name().value());
        assertEquals(4, ((GroupItem.SimpleGroupItem) GroupItem.from(4L)).ordinal());
        assertSame(expr, ((GroupItem.SimpleGroupItem) GroupItem.from(expr)).expr());
        assertThrows(NullPointerException.class, () -> GroupItem.from(null));
        assertThrows(IllegalArgumentException.class, () -> GroupItem.from(new Object()));
    }
}
