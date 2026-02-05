package io.sqm.core.walk;

import io.sqm.core.CollateExpr;
import io.sqm.core.ColumnExpr;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link RecursiveNodeVisitor} handling of {@link CollateExpr}.
 */
class CollateExprVisitorTest {

    @Test
    void visit_collate_expr_traverses_child() {
        var expr = CollateExpr.of(ColumnExpr.of("name"), "de-CH");

        var visitor = new RecursiveNodeVisitor<String>() {
            private String result;
            @Override
            protected String defaultResult() {
                return result;
            }
            @Override
            public String visitColumnExpr(ColumnExpr c) {
                result = c.name();
                return defaultResult();
            }
        };

        String result = expr.accept(visitor);
        assertEquals("name", result);
    }
}
