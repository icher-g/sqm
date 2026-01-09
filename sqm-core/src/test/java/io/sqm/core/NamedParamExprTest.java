package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NamedParamExprTest {

    @Test
    void of() {
        var param = NamedParamExpr.of("userId");
        assertNotNull(param);
        assertInstanceOf(NamedParamExpr.class, param);
        assertEquals("userId", param.name());
    }

    @Test
    void name() {
        var param = NamedParamExpr.of("orderId");
        assertEquals("orderId", param.name());
    }

    @Test
    void accept() {
        var param = NamedParamExpr.of("test");
        var visitor = new TestVisitor();
        var result = param.accept(visitor);
        assertTrue(result);
    }

    @Test
    void differentNames() {
        var param1 = NamedParamExpr.of("param1");
        var param2 = NamedParamExpr.of("param2");
        assertNotEquals(param1.name(), param2.name());
    }

    @Test
    void emptyName() {
        var param = NamedParamExpr.of("");
        assertEquals("", param.name());
    }

    static class TestVisitor extends io.sqm.core.walk.RecursiveNodeVisitor<Boolean> {
        @Override
        protected Boolean defaultResult() {
            return false;
        }

        @Override
        public Boolean visitNamedParamExpr(NamedParamExpr node) {
            return true;
        }
    }
}
