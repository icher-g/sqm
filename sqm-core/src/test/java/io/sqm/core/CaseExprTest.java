package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CaseExprTest {

    @Test
    void of() {
        var p = Expression.column("c").lt(1);
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
        var p = Expression.column("c").lt(1);
        var e = Expression.literal(2);
        var kase = CaseExpr.of(List.of(WhenThen.of(p, e))).elseExpr(e);
        assertEquals(2, kase.whens().getFirst().then().matchExpression().literal(l -> l.value()).orElse(null));
        assertEquals(2, kase.elseExpr().matchExpression().literal(l -> l.value()).orElse(null));
    }

    @Test
    void elseValue() {
        var p = Expression.column("c").lt(1);
        var e = Expression.literal(2);
        var kase = CaseExpr.of(List.of(WhenThen.of(p, e))).elseValue(3);
        assertEquals(2, kase.whens().getFirst().then().matchExpression().literal(l -> l.value()).orElse(null));
        assertEquals(3, kase.elseExpr().matchExpression().literal(l -> l.value()).orElse(null));
    }
}