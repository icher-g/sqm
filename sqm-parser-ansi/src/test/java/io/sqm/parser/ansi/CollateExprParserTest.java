package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.parser.AtomicExprParser;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parser tests for expression-level COLLATE.
 */
class CollateExprParserTest {

    private ParseContext ctx;
    private ParseContext testCtx;
    private CollateExprParser parser;

    @BeforeEach
    void setUp() {
        ctx = ParseContext.of(new AnsiSpecs());
        testCtx = ParseContext.of(new TestSpecs());
        parser = new CollateExprParser(new AtomicExprParser());
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

    @Test
    void parses_collate_expression_with_test_specs() {
        var result = testCtx.parse(Expression.class, "name COLLATE de_CH");
        assertTrue(result.ok());

        var expr = assertInstanceOf(io.sqm.core.CollateExpr.class, result.value());
        assertEquals("de_CH", expr.collation());
    }

    @Test
    void parses_collate_with_qualified_name_with_test_specs() {
        var result = testCtx.parse(Expression.class, "name COLLATE pg_catalog.en_US");
        assertTrue(result.ok());

        var expr = assertInstanceOf(io.sqm.core.CollateExpr.class, result.value());
        assertEquals("pg_catalog.en_US", expr.collation());
    }

    @Test
    void collate_missing_name_errors_with_test_specs() {
        var result = testCtx.parse(Expression.class, "name COLLATE");
        assertTrue(result.isError());
    }

    @Test
    void collate_duplicate_errors_with_test_specs() {
        var result = testCtx.parse(Expression.class, "name COLLATE de_CH COLLATE fr_CH");
        assertTrue(result.isError());
    }

    @Test
    void collate_with_trailing_dot_errors_with_test_specs() {
        var result = testCtx.parse(Expression.class, "name COLLATE pg_catalog.");
        assertTrue(result.isError());
    }

    @Test
    void collate_without_left_expression_errors_with_test_specs() {
        var result = testCtx.parse(io.sqm.core.CollateExpr.class, "COLLATE de_CH");
        assertTrue(result.isError());
    }

    @Test
    void collate_parser_rejects_when_feature_disabled() {
        var result = ctx.parse(io.sqm.core.CollateExpr.class, "name COLLATE de_CH");
        assertTrue(result.isError());
    }

    @Test
    void match_returns_false_when_feature_disabled() {
        var cursor = Cursor.of("COLLATE de_CH", ctx.identifierQuoting());
        assertFalse(parser.match(cursor, ctx));
    }

    @Test
    void match_returns_true_when_feature_enabled() {
        var cursor = Cursor.of("COLLATE de_CH", testCtx.identifierQuoting());
        assertTrue(parser.match(cursor, testCtx));
    }
}
