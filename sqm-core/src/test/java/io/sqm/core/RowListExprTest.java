package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RowListExprTest {

    @Test
    void of() {
        var row1 = RowExpr.of(List.of(Expression.literal(1), Expression.literal(2)));
        var row2 = RowExpr.of(List.of(Expression.literal(3), Expression.literal(4)));
        var rows = List.of(row1, row2);
        var rowListExpr = RowListExpr.of(rows);
        
        assertNotNull(rowListExpr);
        assertInstanceOf(RowListExpr.class, rowListExpr);
        assertEquals(2, rowListExpr.rows().size());
        assertEquals(rows, rowListExpr.rows());
    }

    @Test
    void rows() {
        var row1 = RowExpr.of(List.of(Expression.literal("a")));
        var row2 = RowExpr.of(List.of(Expression.literal("b")));
        var row3 = RowExpr.of(List.of(Expression.literal("c")));
        var rows = List.of(row1, row2, row3);
        var rowListExpr = RowListExpr.of(rows);
        
        assertEquals(rows, rowListExpr.rows());
        assertEquals(3, rowListExpr.rows().size());
    }

    @Test
    void accept() {
        var row1 = RowExpr.of(List.of(Expression.literal(1)));
        var rowListExpr = RowListExpr.of(List.of(row1));
        var visitor = new TestVisitor();
        var result = rowListExpr.accept(visitor);
        assertTrue(result);
    }

    @Test
    void singleRow() {
        var row = RowExpr.of(List.of(Expression.literal(1), Expression.literal(2)));
        var rowListExpr = RowListExpr.of(List.of(row));
        assertEquals(1, rowListExpr.rows().size());
    }

    @Test
    void multipleRows() {
        var rows = List.of(
            RowExpr.of(List.of(Expression.literal(1), Expression.literal(2))),
            RowExpr.of(List.of(Expression.literal(3), Expression.literal(4))),
            RowExpr.of(List.of(Expression.literal(5), Expression.literal(6)))
        );
        var rowListExpr = RowListExpr.of(rows);
        assertEquals(3, rowListExpr.rows().size());
    }

    @Test
    void emptyRows() {
        var rowListExpr = RowListExpr.of(List.of());
        assertEquals(0, rowListExpr.rows().size());
    }

    @Test
    void implementsValueSet() {
        var row = RowExpr.of(List.of(Expression.literal(1)));
        var rowListExpr = RowListExpr.of(List.of(row));
        assertInstanceOf(ValueSet.class, rowListExpr);
    }

    static class TestVisitor extends io.sqm.core.walk.RecursiveNodeVisitor<Boolean> {
        @Override
        protected Boolean defaultResult() {
            return false;
        }

        @Override
        public Boolean visitRowListExpr(RowListExpr node) {
            return true;
        }
    }
}
