package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static io.sqm.dsl.Dsl.col;

class WhenThenTest {

    @Test
    void of() {
        var p = col("c").eq(1);
        var e = Expression.literal(1);
        var whenThen = WhenThen.of(p, e);
        assertEquals("c", whenThen.when().matchPredicate()
            .comparison(cmp -> cmp.lhs().matchExpression()
                .column(c -> c.name().value())
                .orElse(null)
            )
            .orElse(null)
        );
        assertEquals(1, whenThen.then().matchExpression()
            .literal(l -> l.value())
            .orElse(null)
        );
    }

    @Test
    void when() {
        var p = col("c").eq(1);
        var e = Expression.literal(1);
        var whenThen = WhenThen.when(p).then(e);
        assertEquals("c", whenThen.when().matchPredicate()
            .comparison(cmp -> cmp.lhs().matchExpression()
                .column(c -> c.name().value())
                .orElse(null)
            )
            .orElse(null)
        );
        assertEquals(1, whenThen.then().matchExpression()
            .literal(l -> l.value())
            .orElse(null)
        );
    }

    @Test
    void then() {
        var p = col("c").eq(1);
        var whenThen = WhenThen.when(p).then(1);
        assertEquals("c", whenThen.when().matchPredicate()
            .comparison(cmp -> cmp.lhs().matchExpression()
                .column(c -> c.name().value())
                .orElse(null)
            )
            .orElse(null)
        );
        assertEquals(1, whenThen.then().matchExpression()
            .literal(l -> l.value())
            .orElse(null)
        );
    }
}

