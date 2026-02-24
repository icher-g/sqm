package io.sqm.parser.ansi;

import io.sqm.core.ColumnExpr;
import io.sqm.core.LiteralExpr;
import io.sqm.core.UnaryPredicate;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static org.junit.jupiter.api.Assertions.*;

class UnaryPredicateParserTest {

    private final ParseContext ctx = ParseContext.of(new AnsiSpecs());
    private final UnaryPredicateParser parser = new UnaryPredicateParser();

    @Test
    void parsesTrueFalseAndColumn() {
        var trueResult = ctx.parse(parser, "TRUE");
        var falseResult = ctx.parse(parser, "FALSE");
        var columnResult = ctx.parse(parser, "flag");

        assertTrue(trueResult.ok());
        assertTrue(falseResult.ok());
        assertTrue(columnResult.ok());

        assertInstanceOf(LiteralExpr.class, trueResult.value().expr());
        assertInstanceOf(LiteralExpr.class, falseResult.value().expr());
        assertInstanceOf(ColumnExpr.class, columnResult.value().expr());
    }

    @Test
    void infixParseWrapsLhs() {
        var lhs = col("a");
        var cur = Cursor.of("ignored", ctx.identifierQuoting());

        var result = parser.parse(lhs, cur, ctx);

        assertTrue(result.ok());
        assertInstanceOf(UnaryPredicate.class, result.value());
        assertSame(lhs, result.value().expr());
    }
}

