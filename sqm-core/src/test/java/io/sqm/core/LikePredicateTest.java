package io.sqm.core;

import org.junit.jupiter.api.Test;

import static io.sqm.core.Expression.*;
import static io.sqm.dsl.Dsl.col;
import static org.junit.jupiter.api.Assertions.*;

class LikePredicateTest {

    @Test
    void of() {
        var like = LikePredicate.of(col("c"), literal("%v%"), null, true);
        assertEquals("c", like.value().matchExpression().column(c -> c.name().value()).orElse(null));
        assertEquals("%v%", like.pattern().matchExpression().literal(l -> l.value()).orElse(null));
        assertNull(like.escape());
        assertTrue(like.negated());
    }

    @Test
    void escape() {
        var like = LikePredicate.of(col("c"), literal("%v%"), true);
        assertEquals("c", like.value().matchExpression().column(c -> c.name().value()).orElse(null));
        assertEquals("%v%", like.pattern().matchExpression().literal(l -> l.value()).orElse(null));
        assertNull(like.escape());
        assertTrue(like.negated());
        like = like.escape(literal("\\"));
        assertEquals("\\", like.escape().matchExpression().literal(l -> l.value()).orElse(null));
    }
}