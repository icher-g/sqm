package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrdinalParamExprTest {

    @Test
    void of() {
        var param = OrdinalParamExpr.of(1);
        assertNotNull(param);
        assertInstanceOf(OrdinalParamExpr.class, param);
        assertEquals(1, param.index());
    }

    @Test
    void index() {
        var param = OrdinalParamExpr.of(5);
        assertEquals(5, param.index());
    }

    @Test
    void accept() {
        var param = OrdinalParamExpr.of(1);
        var visitor = new TestVisitor();
        var result = param.accept(visitor);
        assertTrue(result);
    }

    @Test
    void differentIndices() {
        var param1 = OrdinalParamExpr.of(1);
        var param2 = OrdinalParamExpr.of(2);
        assertNotEquals(param1.index(), param2.index());
    }

    @Test
    void oneBasedIndex() {
        var param = OrdinalParamExpr.of(1);
        assertEquals(1, param.index());
    }

    @Test
    void higherIndex() {
        var param = OrdinalParamExpr.of(100);
        assertEquals(100, param.index());
    }

    static class TestVisitor extends io.sqm.core.walk.RecursiveNodeVisitor<Boolean> {
        @Override
        protected Boolean defaultResult() {
            return false;
        }

        @Override
        public Boolean visitOrdinalParamExpr(OrdinalParamExpr node) {
            return true;
        }
    }
}
