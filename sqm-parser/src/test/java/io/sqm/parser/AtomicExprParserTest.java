package io.sqm.parser;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class AtomicExprParserTest {

    @Test
    void parsesGroupedExpression() {
        var ctx = contextWithAtomicExprParsers();

        var result = ctx.parse(Expression.class, "(col)");

        assertTrue(result.ok());
        assertInstanceOf(ColumnExpr.class, result.value());
    }

    @Test
    void parsesUnaryMinusExpression() {
        var ctx = contextWithAtomicExprParsers();

        var result = ctx.parse(Expression.class, "-col");

        assertTrue(result.ok());
        assertInstanceOf(NegativeArithmeticExpr.class, result.value());
    }

    @Test
    void parsesPostfixChain() {
        var ctx = contextWithAtomicExprParsers();

        var result = ctx.parse(Expression.class, "col cast sub slice");

        assertTrue(result.ok());
        var slice = assertInstanceOf(ArraySliceExpr.class, result.value());
        assertInstanceOf(ArraySubscriptExpr.class, slice.base());
    }

    @Test
    void errorsOnUnsupportedToken() {
        var ctx = contextWithAtomicExprParsers();

        var result = ctx.parse(Expression.class, "@");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Unexpected character"));
    }

    private static ParseContext contextWithAtomicExprParsers() {
        var repo = new DefaultParsersRepository()
            .register(Expression.class, new AtomicExprWrapper())
            .register(NegativeArithmeticExpr.class, new NegativeArithmeticExprParser())
            .register(CaseExpr.class, new NoMatchParser<>(CaseExpr.class))
            .register(CastExpr.class, new CastExprParser())
            .register(ArrayExpr.class, new NoMatchParser<>(ArrayExpr.class))
            .register(FunctionExpr.class, new NoMatchParser<>(FunctionExpr.class))
            .register(AnonymousParamExpr.class, new NoMatchParser<>(AnonymousParamExpr.class))
            .register(NamedParamExpr.class, new NoMatchParser<>(NamedParamExpr.class))
            .register(OrdinalParamExpr.class, new NoMatchParser<>(OrdinalParamExpr.class))
            .register(QueryExpr.class, new NoMatchParser<>(QueryExpr.class))
            .register(RowExpr.class, new NoMatchParser<>(RowExpr.class))
            .register(ColumnExpr.class, new ColumnExprParser())
            .register(LiteralExpr.class, new LiteralExprParser())
            .register(ArraySubscriptExpr.class, new ArraySubscriptExprParser())
            .register(ArraySliceExpr.class, new ArraySliceExprParser());
        return TestSupport.context(repo);
    }

    private static final class AtomicExprWrapper implements Parser<Expression> {
        @Override
        public ParseResult<? extends Expression> parse(Cursor cur, ParseContext ctx) {
            return new AtomicExprParser().parse(cur, ctx);
        }

        @Override
        public Class<Expression> targetType() {
            return Expression.class;
        }
    }

    private static final class NegativeArithmeticExprParser implements Parser<NegativeArithmeticExpr> {
        @Override
        public ParseResult<? extends NegativeArithmeticExpr> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected -", t -> t.type() == TokenType.OPERATOR && "-".equals(t.lexeme()));
            var inner = ctx.parse(Expression.class, cur);
            if (inner.isError()) {
                return ParseResult.error(inner);
            }
            return ParseResult.ok(NegativeArithmeticExpr.of(inner.value()));
        }

        @Override
        public Class<NegativeArithmeticExpr> targetType() {
            return NegativeArithmeticExpr.class;
        }
    }

    private static final class ColumnExprParser implements MatchableParser<ColumnExpr> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.IDENT);
        }

        @Override
        public ParseResult<? extends ColumnExpr> parse(Cursor cur, ParseContext ctx) {
            var token = cur.expect("Expected identifier", TokenType.IDENT);
            return ParseResult.ok(ColumnExpr.of(token.lexeme()));
        }

        @Override
        public Class<ColumnExpr> targetType() {
            return ColumnExpr.class;
        }
    }

    private static final class LiteralExprParser implements MatchableParser<LiteralExpr> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.NUMBER);
        }

        @Override
        public ParseResult<? extends LiteralExpr> parse(Cursor cur, ParseContext ctx) {
            var token = cur.expect("Expected number", TokenType.NUMBER);
            return ParseResult.ok(Expression.literal(Integer.parseInt(token.lexeme())));
        }

        @Override
        public Class<LiteralExpr> targetType() {
            return LiteralExpr.class;
        }
    }

    private static final class CastExprParser implements MatchableParser<CastExpr>, InfixParser<Expression, CastExpr> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.CAST);
        }

        @Override
        public ParseResult<CastExpr> parse(Expression lhs, Cursor cur, ParseContext ctx) {
            cur.expect("Expected cast marker", TokenType.CAST);
            var type = TypeName.of(List.of("int"), null, List.of(), 0, TimeZoneSpec.NONE);
            return ParseResult.ok(CastExpr.of(lhs, type));
        }

        @Override
        public ParseResult<? extends CastExpr> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected cast marker", TokenType.IDENT);
            var type = TypeName.of(List.of("int"), null, List.of(), 0, TimeZoneSpec.NONE);
            return ParseResult.ok(CastExpr.of(Expression.literal(1), type));
        }

        @Override
        public Class<CastExpr> targetType() {
            return CastExpr.class;
        }
    }

    private static final class ArraySubscriptExprParser implements MatchableParser<ArraySubscriptExpr>, InfixParser<Expression, ArraySubscriptExpr> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.IDENT) && "sub".equalsIgnoreCase(cur.peek().lexeme());
        }

        @Override
        public ParseResult<ArraySubscriptExpr> parse(Expression lhs, Cursor cur, ParseContext ctx) {
            cur.expect("Expected sub marker", TokenType.IDENT);
            return ParseResult.ok(ArraySubscriptExpr.of(lhs, Expression.literal(1)));
        }

        @Override
        public ParseResult<? extends ArraySubscriptExpr> parse(Cursor cur, ParseContext ctx) {
            return ParseResult.error("Unexpected array subscript parse", -1);
        }

        @Override
        public Class<ArraySubscriptExpr> targetType() {
            return ArraySubscriptExpr.class;
        }
    }

    private static final class ArraySliceExprParser implements MatchableParser<ArraySliceExpr>, InfixParser<Expression, ArraySliceExpr> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.IDENT) && "slice".equalsIgnoreCase(cur.peek().lexeme());
        }

        @Override
        public ParseResult<ArraySliceExpr> parse(Expression lhs, Cursor cur, ParseContext ctx) {
            cur.expect("Expected slice marker", TokenType.IDENT);
            return ParseResult.ok(ArraySliceExpr.of(lhs, Expression.literal(1), Expression.literal(2)));
        }

        @Override
        public ParseResult<? extends ArraySliceExpr> parse(Cursor cur, ParseContext ctx) {
            return ParseResult.error("Unexpected array slice parse", -1);
        }

        @Override
        public Class<ArraySliceExpr> targetType() {
            return ArraySliceExpr.class;
        }
    }

    private static final class NoMatchParser<T extends Node> implements MatchableParser<T> {
        private final Class<T> type;

        private NoMatchParser(Class<T> type) {
            this.type = type;
        }

        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return false;
        }

        @Override
        public ParseResult<? extends T> parse(Cursor cur, ParseContext ctx) {
            return ParseResult.error("No match for " + type.getSimpleName(), -1);
        }

        @Override
        public Class<T> targetType() {
            return type;
        }
    }
}
