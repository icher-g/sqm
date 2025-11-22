package io.sqm.render.ansi.transform;

import io.sqm.core.AnonymousParamExpr;
import io.sqm.core.ParamExpr;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnonymousParamsTransformerTest {

    @Test
    void namedParametersWithSameNameSharePosition() {
        AnonymousParamsTransformer t = new AnonymousParamsTransformer();

        ParamExpr p1 = ParamExpr.named("id");
        ParamExpr p2 = ParamExpr.named("id");

        p1.accept(t);
        p2.accept(t);

        assertEquals(1, t.paramsByIndex().size(), "only one logical parameter expected");
    }

    @Test
    void ordinalParametersWithSameIndexSharePosition() {
        AnonymousParamsTransformer t = new AnonymousParamsTransformer();

        ParamExpr p1 = ParamExpr.ordinal(3);
        ParamExpr p2 = ParamExpr.ordinal(3);

        p1.accept(t);
        p2.accept(t);

        assertEquals(1, t.paramsByIndex().size());
    }

    @Test
    void anonymousParametersAlwaysGetNewPosition() {
        AnonymousParamsTransformer t = new AnonymousParamsTransformer();

        ParamExpr p1 = AnonymousParamExpr.of();
        ParamExpr p2 = AnonymousParamExpr.of();

        p1.accept(t);
        p2.accept(t);

        assertEquals(2, t.paramsByIndex().size());
    }

    @Test
    void mixedParametersAssignedInOrderOfFirstAppearance() {
        AnonymousParamsTransformer t = new AnonymousParamsTransformer();

        ParamExpr p1 = ParamExpr.named("a");    // first
        ParamExpr p2 = ParamExpr.ordinal(3);    // second
        ParamExpr p3 = AnonymousParamExpr.of();// third
        ParamExpr p4 = ParamExpr.named("b");    // fourth
        ParamExpr p5 = ParamExpr.named("a");    // same as first

        p1.accept(t);
        p2.accept(t);
        p3.accept(t);
        p4.accept(t);
        p5.accept(t);

        // appearance order should be 1..4
        assertEquals(p1, t.paramsByIndex().get(1));
        assertEquals(p2, t.paramsByIndex().get(2));
        assertEquals(p3, t.paramsByIndex().get(3));
        assertEquals(p4, t.paramsByIndex().get(4));

        // repeated "a" must reuse position 1
        assertEquals(p5, t.paramsByIndex().get(1));
        assertEquals(4, t.paramsByIndex().size());
    }

    @Test
    void nestedQueriesShareGlobalPositionSequence() {
        AnonymousParamsTransformer t = new AnonymousParamsTransformer();

        // Simulate nested usage by calling accept in "outer" then "inner" order.
        ParamExpr outerTenant = ParamExpr.named("tenantId");
        ParamExpr innerTenant = ParamExpr.named("tenantId");
        ParamExpr innerFilter = ParamExpr.ordinal(1);

        outerTenant.accept(t);
        innerTenant.accept(t);
        innerFilter.accept(t);

        // Both named "tenantId" share first position.
        assertEquals(outerTenant, t.paramsByIndex().get(1));
        assertEquals(innerTenant, t.paramsByIndex().get(1));

        // Ordinal(1) comes after first appearance of "tenantId".
        assertEquals(innerFilter, t.paramsByIndex().get(2));

        assertEquals(2, t.paramsByIndex().size());
    }
}