package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RowExprTest {

    @Test
    void of() {
        List<Expression> items = new ArrayList<>();
        items.add(Expression.literal(1));
        items.add(Expression.literal(2));
        items.add(Expression.literal(3));
        var rowExpr = RowExpr.of(items);
        
        assertNotNull(rowExpr);
        assertInstanceOf(RowExpr.class, rowExpr);
        assertEquals(3, rowExpr.items().size());
        assertEquals(items, rowExpr.items());
    }

    @Test
    void items() {
        List<Expression> items = new ArrayList<>();
        items.add(Expression.literal("a"));
        items.add(Expression.literal("b"));
        var rowExpr = RowExpr.of(items);
        assertEquals(items, rowExpr.items());
        assertEquals(2, rowExpr.items().size());
    }

    @Test
    void accept() {
        List<Expression> items = new ArrayList<>();
        items.add(Expression.literal(1));
        var rowExpr = RowExpr.of(items);
        var visitor = new TestVisitor();
        var result = rowExpr.accept(visitor);
        assertTrue(result);
    }

    @Test
    void singleItem() {
        List<Expression> items = new ArrayList<>();
        items.add(Expression.literal(42));
        var rowExpr = RowExpr.of(items);
        assertEquals(1, rowExpr.items().size());
    }

    @Test
    void multipleItems() {
        List<Expression> items = new ArrayList<>();
        items.add(Expression.literal(1));
        items.add(Expression.literal(2));
        items.add(Expression.literal(3));
        items.add(Expression.literal(4));
        items.add(Expression.literal(5));
        var rowExpr = RowExpr.of(items);
        assertEquals(5, rowExpr.items().size());
    }

    @Test
    void mixedExpressions() {
        List<Expression> items = new ArrayList<>();
        items.add(Expression.literal(1));
        items.add(Expression.column("x"));
        items.add(Expression.literal("test"));
        var rowExpr = RowExpr.of(items);
        assertEquals(3, rowExpr.items().size());
    }

    @Test
    void implementsValueSet() {
        List<Expression> items = new ArrayList<>();
        items.add(Expression.literal(1));
        var rowExpr = RowExpr.of(items);
        assertInstanceOf(ValueSet.class, rowExpr);
    }

    static class TestVisitor extends io.sqm.core.walk.RecursiveNodeVisitor<Boolean> {
        @Override
        protected Boolean defaultResult() {
            return false;
        }

        @Override
        public Boolean visitRowExpr(RowExpr node) {
            return true;
        }
    }
}
