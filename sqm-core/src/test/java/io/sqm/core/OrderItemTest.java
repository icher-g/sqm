package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderItemTest {

    @Test
    void of() {
        var item = OrderItem.of(1, Direction.ASC, Nulls.FIRST, "collate");
        assertInstanceOf(OrderItem.class, item);
        assertEquals(1, item.ordinal());
        assertEquals(Direction.ASC, item.direction());
        assertEquals(Nulls.FIRST, item.nulls());
        assertEquals("collate", item.collate());
        item = OrderItem.of(Expression.literal(1), Direction.ASC, Nulls.FIRST, "collate");
        assertInstanceOf(OrderItem.class, item);
        assertEquals(1, item.expr().matchExpression().literal(l -> l.value()).orElse(null));
        assertEquals(Direction.ASC, item.direction());
        assertEquals(Nulls.FIRST, item.nulls());
        assertEquals("collate", item.collate());
    }

    @Test
    void nulls() {
        var item = OrderItem.of(1).nulls(Nulls.FIRST);
        assertEquals(Nulls.FIRST, item.nulls());
    }

    @Test
    void collate() {
        var item = OrderItem.of(1).collate("collate");
        assertEquals("collate", item.collate());
    }

    @Test
    void usingOperator() {
        var item = OrderItem.of(Expression.literal(1)).using("<");
        assertEquals("<", item.usingOperator());
    }
}
