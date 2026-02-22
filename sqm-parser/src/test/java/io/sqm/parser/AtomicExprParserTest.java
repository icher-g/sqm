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
    void parsesUnaryPlusExpression() {
        var ctx = contextWithAtomicExprParsers();

        var result = ctx.parse(Expression.class, "+col");

        assertTrue(result.ok());
        var expr = assertInstanceOf(UnaryOperatorExpr.class, result.value());
        assertEquals("+", expr.operator());
    }

    @Test
    void parsesFunctionExpression() {
        var ctx = contextWithAtomicExprParsers();

        var result = ctx.parse(Expression.class, "fn");

        assertTrue(result.ok());
        assertInstanceOf(FunctionExpr.class, result.value());
    }

    @Test
    void parsesAnonymousParamExpression() {
        var ctx = contextWithAtomicExprParsers();

        var result = ctx.parse(Expression.class, "?");

        assertTrue(result.ok());
        assertInstanceOf(AnonymousParamExpr.class, result.value());
    }

    @Test
    void parsesQueryExpression() {
        var ctx = contextWithAtomicExprParsers();

        var result = ctx.parse(Expression.class, "subq");

        assertTrue(result.ok());
        assertInstanceOf(QueryExpr.class, result.value());
    }

    @Test
    void parsesRowExpressionWhenGroupedExpressionIsRejected() {
        var ctx = contextWithGroupedFallbackParsers();

        var result = ctx.parse(Expression.class, "(x)");

        assertTrue(result.ok(), () -> "error: " + result.errorMessage());
        assertInstanceOf(RowExpr.class, result.value());
    }

    @Test
    void errorsOnUnsupportedToken() {
        var ctx = contextWithAtomicExprParsers();

        var result = ctx.parse(Expression.class, ".");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Unsupported expression token"));
    }

    private static ParseContext contextWithAtomicExprParsers() {
        var repo = new DefaultParsersRepository()
            .register(Expression.class, new AtomicExprWrapper())
            .register(NegativeArithmeticExpr.class, new NegativeArithmeticExprParser())
            .register(UnaryOperatorExpr.class, new UnaryOperatorExprParser())
            .register(CaseExpr.class, new NoMatchParser<>(CaseExpr.class))
            .register(CastExpr.class, new CastExprParser())
            .register(ArrayExpr.class, new NoMatchParser<>(ArrayExpr.class))
            .register(FunctionExpr.class, new FunctionExprParser())
            .register(AnonymousParamExpr.class, new AnonymousParamExprParser())
            .register(NamedParamExpr.class, new NoMatchParser<>(NamedParamExpr.class))
            .register(OrdinalParamExpr.class, new NoMatchParser<>(OrdinalParamExpr.class))
            .register(QueryExpr.class, new QueryExprParser())
            .register(RowExpr.class, new RowExprParser())
            .register(ColumnExpr.class, new ColumnExprParser())
            .register(LiteralExpr.class, new LiteralExprParser())
            .register(ArraySubscriptExpr.class, new ArraySubscriptExprParser())
            .register(ArraySliceExpr.class, new ArraySliceExprParser())
            .register(AtTimeZoneExpr.class, new NoMatchParser<>(AtTimeZoneExpr.class))
            .register(AtTimeZoneExpr.class, new NoMatchInfixParser<>(AtTimeZoneExpr.class))
            .register(CollateExpr.class, new NoMatchParser<>(CollateExpr.class))
            .register(CollateExpr.class, new NoMatchInfixParser<>(CollateExpr.class));
        return TestSupport.context(repo);
    }

    private static ParseContext contextWithGroupedFallbackParsers() {
        var repo = new DefaultParsersRepository()
            .register(Expression.class, new GroupedFallbackExpressionParser())
            .register(CaseExpr.class, new NoMatchParser<>(CaseExpr.class))
            .register(CastExpr.class, new NoMatchInfixParser<>(CastExpr.class))
            .register(ArrayExpr.class, new NoMatchParser<>(ArrayExpr.class))
            .register(FunctionExpr.class, new NoMatchParser<>(FunctionExpr.class))
            .register(AnonymousParamExpr.class, new NoMatchParser<>(AnonymousParamExpr.class))
            .register(NamedParamExpr.class, new NoMatchParser<>(NamedParamExpr.class))
            .register(OrdinalParamExpr.class, new NoMatchParser<>(OrdinalParamExpr.class))
            .register(QueryExpr.class, new NoMatchParser<>(QueryExpr.class))
            .register(ArraySubscriptExpr.class, new NoMatchInfixParser<>(ArraySubscriptExpr.class))
            .register(ArraySliceExpr.class, new NoMatchInfixParser<>(ArraySliceExpr.class))
            .register(RowExpr.class, new RowExprParser())
            .register(ColumnExpr.class, new ColumnExprParser())
            .register(LiteralExpr.class, new LiteralExprParser())
            .register(AtTimeZoneExpr.class, new NoMatchParser<>(AtTimeZoneExpr.class))
            .register(AtTimeZoneExpr.class, new NoMatchInfixParser<>(AtTimeZoneExpr.class))
            .register(CollateExpr.class, new NoMatchParser<>(CollateExpr.class))
            .register(CollateExpr.class, new NoMatchInfixParser<>(CollateExpr.class));
        return TestSupport.context(repo);
    }

    private static final class AtomicExprWrapper implements Parser<Expression> {
        @Override
        public ParseResult<? extends Expression> parse(Cursor cur, ParseContext ctx) {
            return new PostfixExprParser(new AtomicExprParser()).parse(cur, ctx);
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

    private static final class UnaryOperatorExprParser implements Parser<UnaryOperatorExpr> {
        @Override
        public ParseResult<? extends UnaryOperatorExpr> parse(Cursor cur, ParseContext ctx) {
            var op = cur.expect("Expected unary operator", TokenType.OPERATOR).lexeme();
            var token = cur.expect("Expected identifier", TokenType.IDENT);
            return ParseResult.ok(UnaryOperatorExpr.of(op, Expression.column(token.lexeme())));
        }

        @Override
        public Class<UnaryOperatorExpr> targetType() {
            return UnaryOperatorExpr.class;
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

    private static final class FunctionExprParser implements MatchableParser<FunctionExpr> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.IDENT) && "fn".equalsIgnoreCase(cur.peek().lexeme());
        }

        @Override
        public ParseResult<? extends FunctionExpr> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected function name", TokenType.IDENT);
            return ParseResult.ok(FunctionExpr.of("fn"));
        }

        @Override
        public Class<FunctionExpr> targetType() {
            return FunctionExpr.class;
        }
    }

    private static final class AnonymousParamExprParser implements MatchableParser<AnonymousParamExpr> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.QMARK);
        }

        @Override
        public ParseResult<? extends AnonymousParamExpr> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected ?", TokenType.QMARK);
            return ParseResult.ok(AnonymousParamExpr.of());
        }

        @Override
        public Class<AnonymousParamExpr> targetType() {
            return AnonymousParamExpr.class;
        }
    }

    private static final class QueryExprParser implements MatchableParser<QueryExpr> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.IDENT) && "subq".equalsIgnoreCase(cur.peek().lexeme());
        }

        @Override
        public ParseResult<? extends QueryExpr> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected subquery marker", TokenType.IDENT);
            return ParseResult.ok(QueryExpr.of(Query.select(Expression.literal(1)).build()));
        }

        @Override
        public Class<QueryExpr> targetType() {
            return QueryExpr.class;
        }
    }

    private static final class RowExprParser implements MatchableParser<RowExpr> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.LPAREN);
        }

        @Override
        public ParseResult<? extends RowExpr> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected (", TokenType.LPAREN);
            var token = cur.expect("Expected row marker", TokenType.IDENT);
            cur.expect("Expected )", TokenType.RPAREN);
            return ParseResult.ok(RowExpr.of(List.of(Expression.column(token.lexeme()))));
        }

        @Override
        public Class<RowExpr> targetType() {
            return RowExpr.class;
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
            cur.expect("Expected CAST", TokenType.CAST);
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

    private static final class GroupedFallbackExpressionParser implements Parser<Expression> {
        @Override
        public ParseResult<? extends Expression> parse(Cursor cur, ParseContext ctx) {
            if (cur.match(TokenType.IDENT) && "x".equalsIgnoreCase(cur.peek().lexeme())) {
                throw new io.sqm.parser.core.ParserException("force grouped fallback", cur.fullPos());
            }
            return new AtomicExprParser().parse(cur, ctx);
        }

        @Override
        public Class<Expression> targetType() {
            return Expression.class;
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

    private static final class NoMatchInfixParser<T extends Node> implements MatchableParser<T>, InfixParser<Expression, T> {
        private final Class<T> type;

        private NoMatchInfixParser(Class<T> type) {
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
        public ParseResult<T> parse(Expression lhs, Cursor cur, ParseContext ctx) {
            return ParseResult.error("No infix match for " + type.getSimpleName(), -1);
        }

        @Override
        public Class<T> targetType() {
            return type;
        }
    }
}
