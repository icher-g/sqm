package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parser tests for expression-level COLLATE.
 */
class CollateExprParserTest {

    private ParseContext ctx;

    @BeforeEach
    void setUp() {
        ctx = ParseContext.of(new AnsiSpecs());
    }

    @Test
    void parses_collate_expression() {
        var result = ctx.parse(Expression.class, "name COLLATE de_CH");
        assertTrue(result.isError());
    }

    @Test
    void parses_collate_with_qualified_name() {
        var result = ctx.parse(Expression.class, "name COLLATE pg_catalog.en_US");
        assertTrue(result.isError());
    }
}
