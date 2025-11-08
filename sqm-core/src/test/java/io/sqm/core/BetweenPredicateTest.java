package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BetweenPredicateTest {

    @Test
    void of() {
        var p = BetweenPredicate.of(Expression.column("c"), Expression.literal(1), Expression.literal(2), true);
        assertEquals("c", p.value().asColumn().orElseThrow().name());
        assertEquals(1, p.lower().asLiteral().orElseThrow().value());
        assertEquals(2, p.upper().asLiteral().orElseThrow().value());
        assertTrue(p.symmetric());
    }

    @Test
    void symmetric() {
        var p = BetweenPredicate.of(Expression.column("c"), Expression.literal(1), Expression.literal(2));
        assertEquals("c", p.value().asColumn().orElseThrow().name());
        assertEquals(1, p.lower().asLiteral().orElseThrow().value());
        assertEquals(2, p.upper().asLiteral().orElseThrow().value());
        assertFalse(p.symmetric());
        p = p.symmetric(true);
        assertTrue(p.symmetric());
    }
}