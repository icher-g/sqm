package io.sqm.parser.postgresql;

import io.sqm.core.*;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals(QualifiedName.of(Identifier.of("de-CH", QuoteStyle.DOUBLE_QUOTE)), expr.collation());
    }

    @Test
    void parses_collate_with_qualified_name() {
        var result = ctx.parse(Expression.class, "name COLLATE pg_catalog.en_US");
        assertTrue(result.ok());

        var expr = assertInstanceOf(CollateExpr.class, result.value());
        assertEquals(QualifiedName.of("pg_catalog", "en_US"), expr.collation());
    }

    @Test
    void collate_without_name_errors() {
        var result = ctx.parse(Expression.class, "name COLLATE");
        assertTrue(result.isError());
    }

    @Test
    void duplicate_collate_errors() {
        var result = ctx.parse(Expression.class, "name COLLATE de_CH COLLATE fr_CH");
        assertTrue(result.isError());
    }

    @Test
    void collate_with_trailing_dot_errors() {
        var result = ctx.parse(Expression.class, "name COLLATE pg_catalog.");
        assertTrue(result.isError());
    }
}

