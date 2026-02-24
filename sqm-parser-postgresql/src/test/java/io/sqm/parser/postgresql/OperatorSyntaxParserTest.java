package io.sqm.parser.postgresql;

import io.sqm.core.BinaryOperatorExpr;
import io.sqm.core.ColumnExpr;
import io.sqm.core.Expression;
import io.sqm.core.QuoteStyle;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parser tests for PostgreSQL OPERATOR(...) infix syntax.
 */
class OperatorSyntaxParserTest {

    private ParseContext ctx;

    @BeforeEach
    void setUp() {
        ctx = ParseContext.of(new PostgresSpecs());
    }

    @Test
    void parses_operator_syntax_without_schema() {
        var result = ctx.parse(Expression.class, "a OPERATOR(+) b");
        assertTrue(result.ok());

        var expr = assertInstanceOf(BinaryOperatorExpr.class, result.value());
        assertEquals("OPERATOR(+)", expr.operator().text());
        assertTrue(expr.operator().operatorKeywordSyntax());
        assertEquals("+", expr.operator().symbol());

        var lhs = assertInstanceOf(ColumnExpr.class, expr.left());
        var rhs = assertInstanceOf(ColumnExpr.class, expr.right());
        assertEquals("a", lhs.name().value());
        assertEquals("b", rhs.name().value());
    }

    @Test
    void parses_operator_syntax_with_schema() {
        var result = ctx.parse(Expression.class, "a OPERATOR(pg_catalog.+) b");
        assertTrue(result.ok());

        var expr = assertInstanceOf(BinaryOperatorExpr.class, result.value());
        assertEquals("OPERATOR(pg_catalog.+)", expr.operator().text());
        assertTrue(expr.operator().operatorKeywordSyntax());
        assertTrue(expr.operator().qualified());
        assertEquals("pg_catalog", expr.operator().schemaName().parts().getFirst().value());
        assertEquals(QuoteStyle.NONE, expr.operator().schemaName().parts().getFirst().quoteStyle());

        var lhs = assertInstanceOf(ColumnExpr.class, expr.left());
        var rhs = assertInstanceOf(ColumnExpr.class, expr.right());
        assertEquals("a", lhs.name().value());
        assertEquals("b", rhs.name().value());
    }

    @Test
    void parses_operator_syntax_with_quoted_schema_preserves_quote_metadata() {
        var result = ctx.parse(Expression.class, "a OPERATOR(\"PgOps\".+) b");
        assertTrue(result.ok());

        var expr = assertInstanceOf(BinaryOperatorExpr.class, result.value());
        assertEquals("OPERATOR(PgOps.+)", expr.operator().text());
        assertTrue(expr.operator().qualified());
        assertEquals(QuoteStyle.DOUBLE_QUOTE, expr.operator().schemaName().parts().getFirst().quoteStyle());
        assertEquals("PgOps", expr.operator().schemaName().parts().getFirst().value());
    }
}

