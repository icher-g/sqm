package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GroupItemTest {

    @Test
    void of() {
        var item = GroupItem.of(1);
        assertEquals(1, item.ordinal());
        item = GroupItem.of(Expression.literal(1));
        assertNotNull(item.expr());
    }

    @Test
    void isOrdinal() {
        var item = GroupItem.of(1);
        assertTrue(item.isOrdinal());
        item = GroupItem.of(Expression.literal(1));
        assertFalse(item.isOrdinal());
    }
}