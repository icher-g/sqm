package io.sqm.parser.postgresql;

import io.sqm.core.BinaryOperatorExpr;
import io.sqm.core.ColumnExpr;
import io.sqm.core.Expression;
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
        assertEquals("OPERATOR(+)", expr.operator());

        var lhs = assertInstanceOf(ColumnExpr.class, expr.left());
        var rhs = assertInstanceOf(ColumnExpr.class, expr.right());
        assertEquals("a", lhs.name());
        assertEquals("b", rhs.name());
    }

    @Test
    void parses_operator_syntax_with_schema() {
        var result = ctx.parse(Expression.class, "a OPERATOR(pg_catalog.+) b");
        assertTrue(result.ok());

        var expr = assertInstanceOf(BinaryOperatorExpr.class, result.value());
        assertEquals("OPERATOR(pg_catalog.+)", expr.operator());

        var lhs = assertInstanceOf(ColumnExpr.class, expr.left());
        var rhs = assertInstanceOf(ColumnExpr.class, expr.right());
        assertEquals("a", lhs.name());
        assertEquals("b", rhs.name());
    }
}
