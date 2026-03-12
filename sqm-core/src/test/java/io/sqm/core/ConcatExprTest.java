package io.sqm.core;

import io.sqm.core.transform.RecursiveNodeTransformer;
import io.sqm.core.walk.RecursiveNodeVisitor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.concat;
import static io.sqm.dsl.Dsl.lit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ConcatExpr")
class ConcatExprTest {

    @Test
    @DisplayName("Create concatenation expression from multiple arguments")
    void createConcatExpression() {
        ConcatExpr expr = ConcatExpr.of(col("first_name"), lit(" "), col("last_name"));

        assertEquals(3, expr.args().size());
        assertInstanceOf(ColumnExpr.class, expr.args().getFirst());
        assertInstanceOf(LiteralExpr.class, expr.args().get(1));
    }

    @Test
    @DisplayName("Nested concatenation preserves structure")
    void preserveNestedConcatExpression() {
        ConcatExpr expr = concat(col("first_name"), concat(lit(" "), col("last_name")));

        assertEquals(2, expr.args().size());
        assertEquals("first_name", ((ColumnExpr) expr.args().getFirst()).name().value());
        var nested = assertInstanceOf(ConcatExpr.class, expr.args().get(1));
        assertEquals(" ", ((LiteralExpr) nested.args().getFirst()).value());
        assertEquals("last_name", ((ColumnExpr) nested.args().get(1)).name().value());
    }

    @Test
    @DisplayName("Concat requires at least one argument")
    void rejectEmptyArgs() {
        assertThrows(IllegalArgumentException.class, () -> ConcatExpr.of(List.of()));
    }

    @Test
    @DisplayName("Recursive visitor sees every concatenated argument")
    void recursiveVisitorTraversesArgs() {
        var visited = new ArrayList<String>();
        ConcatExpr expr = concat(col("first_name"), lit(" "), col("last_name"));

        expr.accept(new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitColumnExpr(ColumnExpr c) {
                visited.add(c.name().value());
                return null;
            }

            @Override
            public Void visitLiteralExpr(LiteralExpr l) {
                visited.add(String.valueOf(l.value()));
                return null;
            }
        });

        assertEquals(List.of("first_name", " ", "last_name"), visited);
    }

    @Test
    @DisplayName("Recursive transformer preserves identity when unchanged")
    void recursiveTransformerPreservesIdentity() {
        ConcatExpr expr = concat(col("first_name"), lit(" "), col("last_name"));

        Node transformed = expr.accept(new RecursiveNodeTransformer() {
        });

        assertSame(expr, transformed);
    }

    @Test
    @DisplayName("Recursive transformer creates a new node when an argument changes")
    void recursiveTransformerRebuildsWhenChanged() {
        ConcatExpr expr = concat(col("first_name"), lit(" "), col("last_name"));

        Node transformed = expr.accept(new RecursiveNodeTransformer() {
            @Override
            public Node visitColumnExpr(ColumnExpr c) {
                if ("last_name".equals(c.name().value())) {
                    return col("surname");
                }
                return c;
            }
        });

        var concatExpr = assertInstanceOf(ConcatExpr.class, transformed);
        assertNotSame(expr, concatExpr);
        assertEquals("surname", ((ColumnExpr) concatExpr.args().getLast()).name().value());
    }
}
