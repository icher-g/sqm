package io.sqm.parser.postgresql;

import io.sqm.core.CollateExpr;
import io.sqm.core.Expression;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parser tests for PostgreSQL expression-level COLLATE.
 */
class CollateExprParserTest {

    private ParseContext ctx;

    @BeforeEach
    void setUp() {
        ctx = ParseContext.of(new PostgresSpecs());
    }

    @Test
    void parses_collate_expression() {
        var result = ctx.parse(Expression.class, "name COLLATE \"de-CH\"");
        assertTrue(result.ok());

        var expr = assertInstanceOf(CollateExpr.class, result.value());
        assertEquals("de-CH", expr.collation());
    }
}
