package io.sqm.core.transform;

import io.sqm.core.CollateExpr;
import io.sqm.core.ColumnExpr;
import io.sqm.core.Identifier;
import io.sqm.core.Node;
import io.sqm.core.QualifiedName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link RecursiveNodeTransformer} handling of {@link CollateExpr}.
 */
class CollateExprTransformerTest {

    @Test
    void transform_collate_expr_when_child_changes() {
        var expr = ColumnExpr.of(null, Identifier.of("old")).collate("de-CH");

        var transformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitColumnExpr(ColumnExpr c) {
                if ("old".equals(c.name().value())) {
                    return ColumnExpr.of(null, Identifier.of("new"));
                }
                return c;
            }
        };

        var transformed = (CollateExpr) transformer.transform(expr);
        assertEquals("new", ((ColumnExpr) transformed.expr()).name().value());
        assertEquals(QualifiedName.of("de-CH"), transformed.collation());
    }

    @Test
    void transform_collate_expr_returns_same_when_unchanged() {
        var expr = ColumnExpr.of(null, Identifier.of("name")).collate("de-CH");

        var transformer = new RecursiveNodeTransformer() {};
        var transformed = transformer.transform(expr);

        assertSame(expr, transformed);
    }
}
