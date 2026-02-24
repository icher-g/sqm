package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static io.sqm.dsl.Dsl.col;

class CaseExprTest {

    @Test
    void of() {
        var p = col("c").lt(1);
        var e = Expression.literal(2);
        var kase = CaseExpr.of(List.of(WhenThen.of(p, e)), e);
        assertEquals(2, kase.whens().getFirst().then().matchExpression().literal(l -> l.value()).orElse(null));
        assertEquals(2, kase.elseExpr().matchExpression().literal(l -> l.value()).orElse(null));
        kase = CaseExpr.of(WhenThen.of(p, e));
        assertEquals(2, kase.whens().getFirst().then().matchExpression().literal(l -> l.value()).orElse(null));
        assertNull(kase.elseExpr());
    }

    @Test
    void elseExpr() {
        var p = col("c").lt(1);
        var e = Expression.literal(2);
        var kase = CaseExpr.of(List.of(WhenThen.of(p, e))).elseExpr(e);
        assertEquals(2, kase.whens().getFirst().then().matchExpression().literal(l -> l.value()).orElse(null));
        assertEquals(2, kase.elseExpr().matchExpression().literal(l -> l.value()).orElse(null));
    }

    @Test
    void elseValue() {
        var p = col("c").lt(1);
        var e = Expression.literal(2);
        var kase = CaseExpr.of(List.of(WhenThen.of(p, e))).elseValue(3);
        assertEquals(2, kase.whens().getFirst().then().matchExpression().literal(l -> l.value()).orElse(null));
        assertEquals(3, kase.elseExpr().matchExpression().literal(l -> l.value()).orElse(null));
    }
}
