package io.sqm.parser;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.IdentifierQuoting;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class JoinParserTest {

    @Test
    void parseKindDefaultsToInnerWhenNoPrefix() {
        var cur = Cursor.of("JOIN t", IdentifierQuoting.of('"'));
        var kind = JoinParser.parseKind(cur);

        assertEquals(JoinKind.INNER, kind);
        assertTrue(cur.match(TokenType.JOIN));
    }

    @Test
    void parseKindConsumesOuterKeywordWhenPresent() {
        var cur = Cursor.of("LEFT OUTER JOIN t", IdentifierQuoting.of('"'));
        var kind = JoinParser.parseKind(cur);

        assertEquals(JoinKind.LEFT, kind);
        assertTrue(cur.match(TokenType.JOIN));
    }

    @Test
    void parsesCrossJoinBeforeJoinKeyword() {
        var ctx = contextWithJoinParsers();

        var result = ctx.parse(Join.class, "CROSS");

        assertTrue(result.ok());
        assertInstanceOf(CrossJoin.class, result.value());
    }

    @Test
    void parsesNaturalJoinBeforeJoinKeyword() {
        var ctx = contextWithJoinParsers();

        var result = ctx.parse(Join.class, "NATURAL");

        assertTrue(result.ok());
        assertInstanceOf(NaturalJoin.class, result.value());
    }

    @Test
    void parsesUsingJoinAndSetsKind() {
        var ctx = contextWithJoinParsers();

        var result = ctx.parse(Join.class, "LEFT JOIN t USING (c)");

        assertTrue(result.ok());
        var join = assertInstanceOf(UsingJoin.class, result.value());
        assertEquals(JoinKind.LEFT, join.kind());
        assertEquals(List.of("c"), join.usingColumns());
    }

    @Test
    void parsesOnJoinAndSetsKind() {
        var ctx = contextWithJoinParsers();

        var result = ctx.parse(Join.class, "RIGHT JOIN t ON");

        assertTrue(result.ok());
        var join = assertInstanceOf(OnJoin.class, result.value());
        assertEquals(JoinKind.RIGHT, join.kind());
        assertNotNull(join.on());
    }

    @Test
    void errorsWhenJoinTypeNotSupported() {
        var ctx = contextWithJoinParsers();

        var result = ctx.parse(Join.class, "JOIN t WHAT");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("not supported"));
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("WHAT"));
    }

    @Test
    void propagatesTableRefErrors() {
        var ctx = contextWithJoinParsers(new TableRefErrorParser());

        var result = ctx.parse(Join.class, "JOIN t");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("table error"));
    }

    private static ParseContext contextWithJoinParsers() {
        return contextWithJoinParsers(new TableRefOkParser());
    }

    private static ParseContext contextWithJoinParsers(Parser<TableRef> tableRefParser) {
        var repo = new DefaultParsersRepository()
            .register(Join.class, new JoinParser())
            .register(CrossJoin.class, new CrossJoinParser())
            .register(NaturalJoin.class, new NaturalJoinParser())
            .register(UsingJoin.class, new UsingJoinParser())
            .register(OnJoin.class, new OnJoinParser())
            .register(TableRef.class, tableRefParser);
        return TestSupport.context(repo);
    }

    private static final class CrossJoinParser implements MatchableParser<CrossJoin> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.CROSS);
        }

        @Override
        public ParseResult<? extends CrossJoin> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected CROSS", TokenType.CROSS);
            return ParseResult.ok(CrossJoin.of(TableRef.table("x")));
        }

        @Override
        public Class<CrossJoin> targetType() {
            return CrossJoin.class;
        }
    }

    private static final class NaturalJoinParser implements MatchableParser<NaturalJoin> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.NATURAL);
        }

        @Override
        public ParseResult<? extends NaturalJoin> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected NATURAL", TokenType.NATURAL);
            return ParseResult.ok(NaturalJoin.of(TableRef.table("n")));
        }

        @Override
        public Class<NaturalJoin> targetType() {
            return NaturalJoin.class;
        }
    }

    private static final class UsingJoinParser implements MatchableParser<UsingJoin>, InfixParser<TableRef, UsingJoin> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.USING);
        }

        @Override
        public ParseResult<UsingJoin> parse(TableRef lhs, Cursor cur, ParseContext ctx) {
            cur.expect("Expected USING", TokenType.USING);
            cur.expect("Expected (", TokenType.LPAREN);
            var column = cur.expect("Expected column", TokenType.IDENT).lexeme();
            cur.expect("Expected )", TokenType.RPAREN);
            return ParseResult.ok(UsingJoin.of(lhs, JoinKind.INNER, column));
        }

        @Override
        public ParseResult<? extends UsingJoin> parse(Cursor cur, ParseContext ctx) {
            return ParseResult.error("Unexpected USING parse", -1);
        }

        @Override
        public Class<UsingJoin> targetType() {
            return UsingJoin.class;
        }
    }

    private static final class OnJoinParser implements MatchableParser<OnJoin>, InfixParser<TableRef, OnJoin> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.ON);
        }

        @Override
        public ParseResult<OnJoin> parse(TableRef lhs, Cursor cur, ParseContext ctx) {
            cur.expect("Expected ON", TokenType.ON);
            var predicate = UnaryPredicate.of(Expression.literal(true));
            return ParseResult.ok(OnJoin.of(lhs, JoinKind.INNER, predicate));
        }

        @Override
        public ParseResult<? extends OnJoin> parse(Cursor cur, ParseContext ctx) {
            return ParseResult.error("Unexpected ON parse", -1);
        }

        @Override
        public Class<OnJoin> targetType() {
            return OnJoin.class;
        }
    }

    private static final class TableRefOkParser implements Parser<TableRef> {
        @Override
        public ParseResult<? extends TableRef> parse(Cursor cur, ParseContext ctx) {
            var token = cur.expect("Expected table", TokenType.IDENT);
            return ParseResult.ok(TableRef.table(token.lexeme()));
        }

        @Override
        public Class<TableRef> targetType() {
            return TableRef.class;
        }
    }

    private static final class TableRefErrorParser implements Parser<TableRef> {
        @Override
        public ParseResult<? extends TableRef> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected table", TokenType.IDENT);
            return ParseResult.error("table error", 0);
        }

        @Override
        public Class<TableRef> targetType() {
            return TableRef.class;
        }
    }
}
