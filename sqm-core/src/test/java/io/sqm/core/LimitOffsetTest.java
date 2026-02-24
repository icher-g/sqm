package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LimitOffsetTest {

    @Test
    void limit() {
        var lo = LimitOffset.limit(1L);
        assertInstanceOf(LiteralExpr.class, lo.limit());
        assertEquals(1L, ((LiteralExpr) lo.limit()).value());
    }

    @Test
    void offset() {
        var lo = LimitOffset.offset(1L);
        assertInstanceOf(LiteralExpr.class, lo.offset());
        assertEquals(1L, ((LiteralExpr) lo.offset()).value());
    }

    @Test
    void equals_treats_numeric_literal_types_as_equal() {
        var lo1 = LimitOffset.of(Expression.literal(1), null);
        var lo2 = LimitOffset.of(Expression.literal(1L), null);

        assertEquals(lo1, lo2);
        assertEquals(lo1.hashCode(), lo2.hashCode());
    }

    @Test
    void equals_with_limit_all_and_offset() {
        var lo1 = LimitOffset.of(null, Expression.literal(5), true);
        var lo2 = LimitOffset.of(null, Expression.literal(5L), true);

        assertEquals(lo1, lo2);
        assertEquals(lo1.hashCode(), lo2.hashCode());
        assertTrue(lo1.limitAll());
    }

    @Test
    void rejects_limit_all_with_limit_expression() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> LimitOffset.of(Expression.literal(1L), null, true));
        assertTrue(ex.getMessage().contains("limitAll"));
    }

    @Test
    void rejects_negative_literal_limit() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> LimitOffset.of(Expression.literal(-1L), null));
        assertTrue(ex.getMessage().contains("limit"));
    }

    @Test
    void rejects_negative_literal_offset() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> LimitOffset.of(null, Expression.literal(-1L)));
        assertTrue(ex.getMessage().contains("offset"));
    }

    @Test
    void equals_uses_expression_equality_for_non_literals() {
        var lo1 = LimitOffset.of(ColumnExpr.of(null, Identifier.of("a")), null);
        var lo2 = LimitOffset.of(ColumnExpr.of(null, Identifier.of("a")), null);

        assertEquals(lo1, lo2);
        assertEquals(lo1.hashCode(), lo2.hashCode());
    }
}
