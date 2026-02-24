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
        var expr = ColumnExpr.of(null, Identifier.of("name")).collate("de-CH");

        assertEquals(QualifiedName.of("de-CH"), expr.collation());
        assertInstanceOf(ColumnExpr.class, expr.expr());
    }

    @Test
    void rejects_blank_collation() {
        assertThrows(IllegalArgumentException.class, () -> ColumnExpr.of(null, Identifier.of("name")).collate(" "));
    }

    @Test
    void rejects_null_collation() {
        assertThrows(NullPointerException.class, () -> ColumnExpr.of(null, Identifier.of("name")).collate(null));
    }

    @Test
    void rejects_null_expression() {
        assertThrows(NullPointerException.class, () -> CollateExpr.of(null, QualifiedName.of("de-CH")));
    }
}
