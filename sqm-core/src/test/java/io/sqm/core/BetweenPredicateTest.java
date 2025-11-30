package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BetweenPredicateTest {

    @Test
    void of() {
        var p = BetweenPredicate.of(Expression.column("c"), Expression.literal(1), Expression.literal(2), true, false);
        assertEquals("c", p.value().matchExpression().column(c -> c.name()).orElse(null));
        assertEquals(1, p.lower().matchExpression().literal(l -> l.value()).orElse(null));
        assertEquals(2, p.upper().matchExpression().literal(l -> l.value()).orElse(null));
        assertTrue(p.symmetric());
    }

    @Test
    void symmetric() {
        var p = BetweenPredicate.of(Expression.column("c"), Expression.literal(1), Expression.literal(2));
        assertEquals("c", p.value().matchExpression().column(c -> c.name()).orElse(null));
        assertEquals(1, p.lower().matchExpression().literal(l -> l.value()).orElse(null));
        assertEquals(2, p.upper().matchExpression().literal(l -> l.value()).orElse(null));
        assertFalse(p.symmetric());
        p = p.symmetric(true);
        assertTrue(p.symmetric());
    }
}