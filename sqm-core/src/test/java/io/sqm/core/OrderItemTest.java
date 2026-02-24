package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderItemTest {

    @Test
    void of() {
        var item = OrderItem.of(1).asc().nullsFirst().collate("collate");
        assertInstanceOf(OrderItem.class, item);
        assertEquals(1, item.ordinal());
        assertEquals(Direction.ASC, item.direction());
        assertEquals(Nulls.FIRST, item.nulls());
        assertEquals(QualifiedName.of("collate"), item.collate());
        item = OrderItem.of(Expression.literal(1)).asc().nullsFirst().collate("collate");
        assertInstanceOf(OrderItem.class, item);
        assertEquals(1, item.expr().matchExpression().literal(l -> l.value()).orElse(null));
        assertEquals(Direction.ASC, item.direction());
        assertEquals(Nulls.FIRST, item.nulls());
        assertEquals(QualifiedName.of("collate"), item.collate());
    }

    @Test
    void nulls() {
        var item = OrderItem.of(1).nulls(Nulls.FIRST);
        assertEquals(Nulls.FIRST, item.nulls());
    }

    @Test
    void collate() {
        var item = OrderItem.of(1).collate("collate");
        assertEquals(QualifiedName.of("collate"), item.collate());
    }

    @Test
    void collateRejectsNullAndBlank() {
        assertThrows(NullPointerException.class, () -> OrderItem.of(1).collate((String) null));
        assertThrows(IllegalArgumentException.class, () -> OrderItem.of(1).collate("   "));
    }

    @Test
    void usingOperator() {
        var item = OrderItem.of(Expression.literal(1)).using("<");
        assertEquals("<", item.usingOperator());
    }

    @Test
    void of_with_all_fields() {
        var item = OrderItem.of(Expression.literal(1), null, Direction.DESC, Nulls.LAST, QualifiedName.of("collate"), ">");
        assertEquals(Direction.DESC, item.direction());
        assertEquals(Nulls.LAST, item.nulls());
        assertEquals(QualifiedName.of("collate"), item.collate());
        assertEquals(">", item.usingOperator());
    }

    @Test
    void of_ordinal_with_using_operator() {
        var item = OrderItem.of(3).using("<>").nullsDefault().collate("de_DE");
        assertEquals(3, item.ordinal());
        assertEquals("<>", item.usingOperator());
        assertEquals(Nulls.DEFAULT, item.nulls());
        assertEquals(QualifiedName.of("de_DE"), item.collate());
    }
}
