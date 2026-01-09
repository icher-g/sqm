package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParamExprTest {

    @Test
    void anonymous() {
        var param = ParamExpr.anonymous();
        assertInstanceOf(AnonymousParamExpr.class, param);
    }

    @Test
    void named() {
        var param = ParamExpr.named("userId");
        assertInstanceOf(NamedParamExpr.class, param);
        assertEquals("userId", param.name());
    }

    @Test
    void ordinal() {
        var param = ParamExpr.ordinal(1);
        assertInstanceOf(OrdinalParamExpr.class, param);
        assertEquals(1, param.index());
    }

    @Test
    void matchParam() {
        var param = ParamExpr.named("id");
        var match = param.matchParam();
        assertNotNull(match);
    }
}
