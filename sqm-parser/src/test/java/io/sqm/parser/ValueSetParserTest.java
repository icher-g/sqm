package io.sqm.parser;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValueSetParserTest {

    @Test
    void prefersQueryExprOverRowListAndRow() {
        var ctx = contextWithValueSetParsers();
        var result = ctx.parse(ValueSet.class, "query");

        assertTrue(result.ok());
        assertInstanceOf(QueryExpr.class, result.value());
    }

    @Test
    void prefersRowListOverRowExpr() {
        var ctx = contextWithValueSetParsers();
        var result = ctx.parse(ValueSet.class, "rows");

        assertTrue(result.ok());
        assertInstanceOf(RowListExpr.class, result.value());
    }

    @Test
    void fallsBackToRowExpr() {
        var ctx = contextWithValueSetParsers();
        var result = ctx.parse(ValueSet.class, "row");

        assertTrue(result.ok());
        assertInstanceOf(RowExpr.class, result.value());
    }

    private static ParseContext contextWithValueSetParsers() {
        var repo = new DefaultParsersRepository()
            .register(ValueSet.class, new ValueSetParser())
            .register(QueryExpr.class, new QueryExprParser())
            .register(RowListExpr.class, new RowListExprParser())
            .register(RowExpr.class, new RowExprParser());
        return TestSupport.context(repo);
    }

    private static final class QueryExprParser implements MatchableParser<QueryExpr> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.IDENT) && "query".equals(cur.peek().lexeme());
        }

        @Override
        public ParseResult<? extends QueryExpr> parse(Cursor cur, ParseContext ctx) {
            cur.advance();
            return ParseResult.ok(QueryExpr.of(SelectQuery.of()));
        }

        @Override
        public Class<QueryExpr> targetType() {
            return QueryExpr.class;
        }
    }

    private static final class RowListExprParser implements MatchableParser<RowListExpr> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.ROWS);
        }

        @Override
        public ParseResult<? extends RowListExpr> parse(Cursor cur, ParseContext ctx) {
            cur.advance();
            var row = RowExpr.of(List.of(Expression.literal(1)));
            return ParseResult.ok(RowListExpr.of(List.of(row)));
        }

        @Override
        public Class<RowListExpr> targetType() {
            return RowListExpr.class;
        }
    }

    private static final class RowExprParser implements io.sqm.parser.spi.Parser<RowExpr> {
        @Override
        public ParseResult<? extends RowExpr> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected ROW", TokenType.ROW);
            return ParseResult.ok(RowExpr.of(List.of(Expression.literal(1))));
        }

        @Override
        public Class<RowExpr> targetType() {
            return RowExpr.class;
        }
    }
}
