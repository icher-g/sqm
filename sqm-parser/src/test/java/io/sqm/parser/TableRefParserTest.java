package io.sqm.parser;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static io.sqm.dsl.Dsl.func;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.*;

class TableRefParserTest {

    @Test
    void parsesLateralFirst() {
        var ctx = contextWithTableRefParsers();

        var result = ctx.parse(TableRef.class, "LATERAL");

        assertTrue(result.ok());
        assertInstanceOf(Lateral.class, result.value());
    }

    @Test
    void parsesParenthesizedQueryTable() {
        var ctx = contextWithTableRefParsers();

        var result = ctx.parse(TableRef.class, "(query)");

        assertTrue(result.ok());
        assertInstanceOf(QueryTable.class, result.value());
    }

    @Test
    void parsesParenthesizedValuesTable() {
        var ctx = contextWithTableRefParsers();

        var result = ctx.parse(TableRef.class, "(values)");

        assertTrue(result.ok());
        assertInstanceOf(ValuesTable.class, result.value());
    }

    @Test
    void parsesParenthesizedFunctionTable() {
        var ctx = contextWithTableRefParsers();

        var result = ctx.parse(TableRef.class, "(func)");

        assertTrue(result.ok());
        assertInstanceOf(FunctionTable.class, result.value());
    }

    @Test
    void parsesParenthesizedTable() {
        var ctx = contextWithTableRefParsers();

        var result = ctx.parse(TableRef.class, "(table)");

        assertTrue(result.ok());
        assertInstanceOf(Table.class, result.value());
    }

    @Test
    void parsesStandaloneValuesAndTable() {
        var ctx = contextWithTableRefParsers();

        var values = ctx.parse(TableRef.class, "values");
        var table = ctx.parse(TableRef.class, "table");

        assertTrue(values.ok());
        assertTrue(table.ok());
        assertInstanceOf(ValuesTable.class, values.value());
        assertInstanceOf(Table.class, table.value());
    }

    @Test
    void errorsOnUnexpectedToken() {
        var ctx = contextWithTableRefParsers();

        var result = ctx.parse(TableRef.class, "unknown");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Unexpected table reference token"));
    }

    private static ParseContext contextWithTableRefParsers() {
        var repo = new DefaultParsersRepository()
            .register(TableRef.class, new TableRefParser())
            .register(Lateral.class, new LateralParser())
            .register(QueryTable.class, new QueryTableParser())
            .register(ValuesTable.class, new ValuesTableParser())
            .register(FunctionTable.class, new FunctionTableParser())
            .register(Table.class, new TableParser());
        return TestSupport.context(repo);
    }

    private static final class LateralParser implements MatchableParser<Lateral> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.LATERAL);
        }

        @Override
        public ParseResult<? extends Lateral> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected LATERAL", TokenType.LATERAL);
            return ParseResult.ok(Lateral.of(tbl("t")));
        }

        @Override
        public Class<Lateral> targetType() {
            return Lateral.class;
        }
    }

    private static final class QueryTableParser implements MatchableParser<QueryTable> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.LPAREN)
                && cur.match(TokenType.IDENT, 1)
                && "query".equalsIgnoreCase(cur.peek(1).lexeme());
        }

        @Override
        public ParseResult<? extends QueryTable> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected (", TokenType.LPAREN);
            cur.expect("Expected query marker", TokenType.IDENT);
            cur.expect("Expected )", TokenType.RPAREN);
            return ParseResult.ok(QueryTable.of(Query.select(Expression.literal(1)).build()));
        }

        @Override
        public Class<QueryTable> targetType() {
            return QueryTable.class;
        }
    }

    private static final class ValuesTableParser implements MatchableParser<ValuesTable> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            if (cur.match(TokenType.LPAREN) && cur.match(TokenType.VALUES, 1)) {
                return true;
            }
            return cur.match(TokenType.VALUES);
        }

        @Override
        public ParseResult<? extends ValuesTable> parse(Cursor cur, ParseContext ctx) {
            if (cur.match(TokenType.LPAREN)) {
                cur.expect("Expected (", TokenType.LPAREN);
                cur.expect("Expected VALUES", TokenType.VALUES);
                cur.expect("Expected )", TokenType.RPAREN);
            } else {
                cur.expect("Expected VALUES", TokenType.VALUES);
            }
            var row = RowExpr.of(List.of(Expression.literal(1)));
            return ParseResult.ok(ValuesTable.of(row));
        }

        @Override
        public Class<ValuesTable> targetType() {
            return ValuesTable.class;
        }
    }

    private static final class FunctionTableParser implements MatchableParser<FunctionTable> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.IDENT) && "func".equalsIgnoreCase(cur.peek().lexeme());
        }

        @Override
        public ParseResult<? extends FunctionTable> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected func marker", TokenType.IDENT);
            return ParseResult.ok(func("func").asTable());
        }

        @Override
        public Class<FunctionTable> targetType() {
            return FunctionTable.class;
        }
    }

    private static final class TableParser implements MatchableParser<Table> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.IDENT) && "table".equalsIgnoreCase(cur.peek().lexeme());
        }

        @Override
        public ParseResult<? extends Table> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected table name", TokenType.IDENT);
            return ParseResult.ok(tbl("table"));
        }

        @Override
        public Class<Table> targetType() {
            return Table.class;
        }
    }
}

