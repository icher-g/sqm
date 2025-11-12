package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LikePredicateTest {

    @Test
    void of() {
        var like = LikePredicate.of(Expression.column("c"), Expression.literal("%v%"), null, true);
        assertEquals("c", like.value().matchExpression().column(c -> c.name()).orElse(null));
        assertEquals("%v%", like.pattern().matchExpression().literal(l -> l.value()).orElse(null));
        assertNull(like.escape());
        assertTrue(like.negated());
    }

    @Test
    void escape() {
        var like = LikePredicate.of(Expression.column("c"), Expression.literal("%v%"), true);
        assertEquals("c", like.value().matchExpression().column(c -> c.name()).orElse(null));
        assertEquals("%v%", like.pattern().matchExpression().literal(l -> l.value()).orElse(null));
        assertNull(like.escape());
        assertTrue(like.negated());
        like = like.escape(Expression.literal("\\"));
        assertEquals("\\", like.escape().matchExpression().literal(l -> l.value()).orElse(null));
    }
}