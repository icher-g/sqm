package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link CollateExpr}.
 */
class CollateExprTest {

    @Test
    void creates_collate_expression() {
        var expr = CollateExpr.of(ColumnExpr.of("name"), "de-CH");

        assertEquals("de-CH", expr.collation());
        assertInstanceOf(ColumnExpr.class, expr.expr());
    }

    @Test
    void rejects_blank_collation() {
        assertThrows(IllegalArgumentException.class, () -> CollateExpr.of(ColumnExpr.of("name"), " "));
    }

    @Test
    void rejects_null_collation() {
        assertThrows(NullPointerException.class, () -> CollateExpr.of(ColumnExpr.of("name"), null));
    }

    @Test
    void rejects_null_expression() {
        assertThrows(NullPointerException.class, () -> CollateExpr.of(null, "de-CH"));
    }
}
