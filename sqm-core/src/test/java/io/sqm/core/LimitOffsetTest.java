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
}
