package io.sqm.dsl;

import io.sqm.core.GroupItem;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class DslGroupItemTest {

    @Test
    void buildsSimpleGroupItems() {
        var byName = (GroupItem.SimpleGroupItem) GroupItem.from("c1");
        assertNotNull(byName.expr());

        var byTable = GroupItem.of(col("t", "c1"));
        assertNotNull(byTable.expr());

        var byExpr = GroupItem.of(col("c2"));
        assertNotNull(byExpr.expr());

        var byOrdinal = GroupItem.of(1);
        assertTrue(byOrdinal.isOrdinal());
        assertEquals(1, byOrdinal.ordinal());
    }

    @Test
    void buildsGroupingExtensions() {
        var set = groupingSet("a", "b");
        assertInstanceOf(GroupItem.GroupingSet.class, set);

        var sets = groupingSets(groupingSet("a"));
        assertInstanceOf(GroupItem.GroupingSets.class, sets);

        var rollupItem = rollup("a");
        assertInstanceOf(GroupItem.Rollup.class, rollupItem);

        var cubeItem = cube("a");
        assertInstanceOf(GroupItem.Cube.class, cubeItem);
    }
}
