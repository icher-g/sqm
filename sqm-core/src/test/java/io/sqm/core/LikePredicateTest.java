package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LikePredicateTest {

    @Test
    void of() {
        var like = LikePredicate.of(Expression.column("c"), Expression.literal("%v%"), null, true);
        assertEquals("c", like.value().asColumn().orElseThrow().name());
        assertEquals("%v%", like.pattern().asLiteral().orElseThrow().value());
        assertNull(like.escape());
        assertTrue(like.negated());
    }

    @Test
    void escape() {
        var like = LikePredicate.of(Expression.column("c"), Expression.literal("%v%"), true);
        assertEquals("c", like.value().asColumn().orElseThrow().name());
        assertEquals("%v%", like.pattern().asLiteral().orElseThrow().value());
        assertNull(like.escape());
        assertTrue(like.negated());
        like = like.escape(Expression.literal("\\"));
        assertEquals("\\", like.escape().asLiteral().orElseThrow().value());
    }
}