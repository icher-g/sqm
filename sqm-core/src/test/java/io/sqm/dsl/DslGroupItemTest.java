package io.sqm.dsl;

import io.sqm.core.GroupItem;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class DslGroupItemTest {

    @Test
    void buildsSimpleGroupItems() {
        var byName = (GroupItem.SimpleGroupItem) group("c1");
        assertNotNull(byName.expr());

        var byTable = (GroupItem.SimpleGroupItem) group("t", "c1");
        assertNotNull(byTable.expr());

        var byExpr = (GroupItem.SimpleGroupItem) group(col("c2"));
        assertNotNull(byExpr.expr());

        var byOrdinal = (GroupItem.SimpleGroupItem) group(1);
        assertTrue(byOrdinal.isOrdinal());
        assertEquals(1, byOrdinal.ordinal());
    }

    @Test
    void buildsGroupingExtensions() {
        var set = groupingSet(group("a"), group("b"));
        assertInstanceOf(GroupItem.GroupingSet.class, set);

        var sets = groupingSets(groupingSet(group("a")));
        assertInstanceOf(GroupItem.GroupingSets.class, sets);

        var rollupItem = rollup(group("a"));
        assertInstanceOf(GroupItem.Rollup.class, rollupItem);

        var cubeItem = cube(group("a"));
        assertInstanceOf(GroupItem.Cube.class, cubeItem);
    }
}
