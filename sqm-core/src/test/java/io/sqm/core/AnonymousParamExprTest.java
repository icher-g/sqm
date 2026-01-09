package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnonymousParamExprTest {

    @Test
    void of() {
        var param = AnonymousParamExpr.of();
        assertNotNull(param);
        assertInstanceOf(AnonymousParamExpr.class, param);
    }

    @Test
    void accept() {
        var param = AnonymousParamExpr.of();
        var visitor = new TestVisitor();
        var result = param.accept(visitor);
        assertTrue(result);
    }

    @Test
    void multipleInstances() {
        var param1 = AnonymousParamExpr.of();
        var param2 = AnonymousParamExpr.of();
        assertNotNull(param1);
        assertNotNull(param2);
    }

    static class TestVisitor extends io.sqm.core.walk.RecursiveNodeVisitor<Boolean> {
        @Override
        protected Boolean defaultResult() {
            return false;
        }

        @Override
        public Boolean visitAnonymousParamExpr(AnonymousParamExpr node) {
            return true;
        }
    }
}
