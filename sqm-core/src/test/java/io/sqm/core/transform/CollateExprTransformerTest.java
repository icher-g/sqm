package io.sqm.core.transform;

import io.sqm.core.CollateExpr;
import io.sqm.core.ColumnExpr;
import io.sqm.core.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link RecursiveNodeTransformer} handling of {@link CollateExpr}.
 */
class CollateExprTransformerTest {

    @Test
    void transform_collate_expr_when_child_changes() {
        var expr = CollateExpr.of(ColumnExpr.of("old"), "de-CH");

        var transformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitColumnExpr(ColumnExpr c) {
                if ("old".equals(c.name())) {
                    return ColumnExpr.of("new");
                }
                return c;
            }
        };

        var transformed = (CollateExpr) transformer.transform(expr);
        assertEquals("new", ((ColumnExpr) transformed.expr()).name());
        assertEquals("de-CH", transformed.collation());
    }

    @Test
    void transform_collate_expr_returns_same_when_unchanged() {
        var expr = CollateExpr.of(ColumnExpr.of("name"), "de-CH");

        var transformer = new RecursiveNodeTransformer() {};
        var transformed = transformer.transform(expr);

        assertSame(expr, transformed);
    }
}
