package io.sqm.parser;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class AtomicPredicateParserTest {

    @Test
    void parsesGroupedPredicate() {
        var ctx = contextWithPredicateParsers();
        var result = ctx.parse(Predicate.class, "(pred)");

        assertTrue(result.ok());
        assertInstanceOf(UnaryPredicate.class, result.value());
    }

    @Test
    void parsesExistsPredicate() {
        var ctx = contextWithPredicateParsers();
        var result = ctx.parse(Predicate.class, "EXISTS");

        assertTrue(result.ok());
        assertInstanceOf(ExistsPredicate.class, result.value());
    }

    @Test
    void parsesNotPredicate() {
        var ctx = contextWithPredicateParsers();
        var result = ctx.parse(Predicate.class, "NOT a");

        assertTrue(result.ok());
        assertInstanceOf(NotPredicate.class, result.value());
    }

    @Test
    void parsesComparisonPredicate() {
        var ctx = contextWithPredicateParsers();
        var result = ctx.parse(Predicate.class, "a = b");

        assertTrue(result.ok());
        assertInstanceOf(ComparisonPredicate.class, result.value());
    }

    @Test
    void parsesAnyPredicate() {
        var ctx = contextWithPredicateParsers();
        var result = ctx.parse(Predicate.class, "a = ANY sub");

        assertTrue(result.ok());
        assertInstanceOf(AnyAllPredicate.class, result.value());
    }

    @Test
    void parsesBetweenPredicate() {
        var ctx = contextWithPredicateParsers();
        var result = ctx.parse(Predicate.class, "a BETWEEN b AND c");

        assertTrue(result.ok());
        var predicate = assertInstanceOf(BetweenPredicate.class, result.value());
        assertFalse(predicate.negated());
    }

    @Test
    void parsesNotBetweenPredicate() {
        var ctx = contextWithPredicateParsers();
        var result = ctx.parse(Predicate.class, "a NOT BETWEEN b AND c");

        assertTrue(result.ok());
        var predicate = assertInstanceOf(BetweenPredicate.class, result.value());
        assertTrue(predicate.negated());
    }

    @Test
    void parsesInPredicate() {
        var ctx = contextWithPredicateParsers();
        var result = ctx.parse(Predicate.class, "a IN (b)");

        assertTrue(result.ok());
        assertInstanceOf(InPredicate.class, result.value());
    }

    @Test
    void parsesNotLikePredicate() {
        var ctx = contextWithPredicateParsers();
        var result = ctx.parse(Predicate.class, "a NOT LIKE b");

        assertTrue(result.ok());
        var predicate = assertInstanceOf(LikePredicate.class, result.value());
        assertTrue(predicate.negated());
    }

    @Test
    void parsesIsNullPredicate() {
        var ctx = contextWithPredicateParsers();
        var result = ctx.parse(Predicate.class, "a IS NULL");

        assertTrue(result.ok());
        assertInstanceOf(IsNullPredicate.class, result.value());
    }

    @Test
    void parsesIsDistinctFromPredicate() {
        var ctx = contextWithPredicateParsers();
        var result = ctx.parse(Predicate.class, "a IS DISTINCT FROM b");

        assertTrue(result.ok());
        var predicate = assertInstanceOf(IsDistinctFromPredicate.class, result.value());
        assertFalse(predicate.negated());
    }

    @Test
    void parsesIsTruePredicate() {
        var ctx = contextWithPredicateParsers();
        var result = ctx.parse(Predicate.class, "a IS TRUE");

        assertTrue(result.ok());
        assertInstanceOf(UnaryPredicate.class, result.value());
    }

    @Test
    void parsesRegexPredicate() {
        var ctx = contextWithPredicateParsers();
        var result = ctx.parse(Predicate.class, "a ~ b");

        assertTrue(result.ok());
        assertInstanceOf(RegexPredicate.class, result.value());
    }

    @Test
    void parsesGroupedExpressionInPredicate() {
        var ctx = contextWithPredicateParsers();
        var result = ctx.parse(Predicate.class, "(a) IN (b)");

        assertTrue(result.ok());
        assertInstanceOf(InPredicate.class, result.value());
    }

    @Test
    void fallsBackToUnaryPredicate() {
        var ctx = contextWithPredicateParsers();
        var result = ctx.parse(Predicate.class, "a");

        assertTrue(result.ok());
        assertInstanceOf(UnaryPredicate.class, result.value());
    }

    @Test
    void errorsOnInvalidIsPredicate() {
        var ctx = contextWithPredicateParsers();
        var result = ctx.parse(Predicate.class, "a IS 1");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Expected NULL, TRUE or FALSE"));
    }

    private static ParseContext contextWithPredicateParsers() {
        var repo = new DefaultParsersRepository()
            .register(Predicate.class, new AtomicPredicateWrapper())
            .register(Expression.class, new ExpressionStubParser())
            .register(ExistsPredicate.class, new ExistsPredicateParser())
            .register(NotPredicate.class, new NotPredicateParser())
            .register(AnyAllPredicate.class, new AnyAllPredicateParser())
            .register(ComparisonPredicate.class, new ComparisonPredicateParser())
            .register(BetweenPredicate.class, new BetweenPredicateParser())
            .register(InPredicate.class, new InPredicateParser())
            .register(LikePredicate.class, new LikePredicateParser())
            .register(IsNullPredicate.class, new IsNullPredicateParser())
            .register(IsDistinctFromPredicate.class, new IsDistinctFromPredicateParser())
            .register(RegexPredicate.class, new RegexPredicateParser())
            .register(UnaryPredicate.class, new UnaryPredicateParser());
        return TestSupport.context(repo);
    }

    private static final class AtomicPredicateWrapper implements Parser<Predicate> {
        @Override
        public ParseResult<? extends Predicate> parse(Cursor cur, ParseContext ctx) {
            return new AtomicPredicateParser().parse(cur, ctx);
        }

        @Override
        public Class<Predicate> targetType() {
            return Predicate.class;
        }
    }

    private static final class ExpressionStubParser implements Parser<Expression> {
        @Override
        public ParseResult<? extends Expression> parse(Cursor cur, ParseContext ctx) {
            if (cur.match(TokenType.LPAREN)) {
                cur.expect("Expected (", TokenType.LPAREN);
                var inner = parse(cur, ctx);
                cur.expect("Expected )", TokenType.RPAREN);
                return inner;
            }
            if (cur.match(TokenType.NUMBER)) {
                var number = cur.expect("Expected number", TokenType.NUMBER);
                return ParseResult.ok(Expression.literal(Integer.parseInt(number.lexeme())));
            }
            var token = cur.expect("Expected identifier", TokenType.IDENT);
            return ParseResult.ok(Expression.column(token.lexeme()));
        }

        @Override
        public Class<Expression> targetType() {
            return Expression.class;
        }
    }

    private static final class ExistsPredicateParser implements Parser<ExistsPredicate> {
        @Override
        public ParseResult<? extends ExistsPredicate> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected EXISTS", TokenType.EXISTS);
            return ParseResult.ok(ExistsPredicate.of(Query.select(Expression.literal(1)).build(), false));
        }

        @Override
        public Class<ExistsPredicate> targetType() {
            return ExistsPredicate.class;
        }
    }

    private static final class NotPredicateParser implements Parser<NotPredicate> {
        @Override
        public ParseResult<? extends NotPredicate> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected NOT", TokenType.NOT);
            var inner = ctx.parse(Predicate.class, cur);
            if (inner.isError()) {
                return ParseResult.error(inner);
            }
            return ParseResult.ok(NotPredicate.of(inner.value()));
        }

        @Override
        public Class<NotPredicate> targetType() {
            return NotPredicate.class;
        }
    }

    private static final class ComparisonPredicateParser implements Parser<ComparisonPredicate>, InfixParser<Expression, ComparisonPredicate> {
        @Override
        public ParseResult<ComparisonPredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
            cur.expect("Expected comparison operator", TokenType.OPERATOR);
            var rhs = ctx.parse(Expression.class, cur);
            if (rhs.isError()) {
                return ParseResult.error(rhs);
            }
            return ParseResult.ok(ComparisonPredicate.of(lhs, ComparisonOperator.EQ, rhs.value()));
        }

        @Override
        public ParseResult<? extends ComparisonPredicate> parse(Cursor cur, ParseContext ctx) {
            return ParseResult.error("Unexpected comparison parse", -1);
        }

        @Override
        public Class<ComparisonPredicate> targetType() {
            return ComparisonPredicate.class;
        }
    }

    private static final class AnyAllPredicateParser implements Parser<AnyAllPredicate>, InfixParser<Expression, AnyAllPredicate> {
        @Override
        public ParseResult<AnyAllPredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
            cur.expect("Expected comparison operator", TokenType.OPERATOR);
            Quantifier quantifier;
            if (cur.match(TokenType.ANY)) {
                cur.advance();
                quantifier = Quantifier.ANY;
            } else {
                cur.expect("Expected ALL", TokenType.ALL);
                quantifier = Quantifier.ALL;
            }
            cur.expect("Expected subquery marker", TokenType.IDENT);
            return ParseResult.ok(AnyAllPredicate.of(lhs, ComparisonOperator.EQ, Query.select(Expression.literal(1)).build(), quantifier));
        }

        @Override
        public ParseResult<? extends AnyAllPredicate> parse(Cursor cur, ParseContext ctx) {
            return ParseResult.error("Unexpected ANY/ALL parse", -1);
        }

        @Override
        public Class<AnyAllPredicate> targetType() {
            return AnyAllPredicate.class;
        }
    }

    private static final class BetweenPredicateParser implements Parser<BetweenPredicate>, InfixParser<Expression, BetweenPredicate> {
        @Override
        public ParseResult<BetweenPredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
            boolean negated = false;
            if (cur.match(TokenType.NOT)) {
                cur.advance();
                negated = true;
            }
            cur.expect("Expected BETWEEN", TokenType.BETWEEN);
            var lower = ctx.parse(Expression.class, cur);
            if (lower.isError()) {
                return ParseResult.error(lower);
            }
            cur.expect("Expected AND", TokenType.AND);
            var upper = ctx.parse(Expression.class, cur);
            if (upper.isError()) {
                return ParseResult.error(upper);
            }
            return ParseResult.ok(BetweenPredicate.of(lhs, lower.value(), upper.value(), false, negated));
        }

        @Override
        public ParseResult<? extends BetweenPredicate> parse(Cursor cur, ParseContext ctx) {
            return ParseResult.error("Unexpected BETWEEN parse", -1);
        }

        @Override
        public Class<BetweenPredicate> targetType() {
            return BetweenPredicate.class;
        }
    }

    private static final class InPredicateParser implements Parser<InPredicate>, InfixParser<Expression, InPredicate> {
        @Override
        public ParseResult<InPredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
            boolean negated = false;
            if (cur.match(TokenType.NOT)) {
                cur.advance();
                negated = true;
            }
            cur.expect("Expected IN", TokenType.IN);
            cur.expect("Expected (", TokenType.LPAREN);
            var value = cur.expect("Expected value", TokenType.IDENT).lexeme();
            cur.expect("Expected )", TokenType.RPAREN);
            var row = RowExpr.of(List.of(Expression.column(value)));
            return ParseResult.ok(InPredicate.of(lhs, row, negated));
        }

        @Override
        public ParseResult<? extends InPredicate> parse(Cursor cur, ParseContext ctx) {
            return ParseResult.error("Unexpected IN parse", -1);
        }

        @Override
        public Class<InPredicate> targetType() {
            return InPredicate.class;
        }
    }

    private static final class LikePredicateParser implements Parser<LikePredicate>, InfixParser<Expression, LikePredicate> {
        @Override
        public ParseResult<LikePredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
            boolean negated = false;
            if (cur.match(TokenType.NOT)) {
                cur.advance();
                negated = true;
            }
            cur.expect("Expected LIKE", TokenType.LIKE);
            var pattern = ctx.parse(Expression.class, cur);
            if (pattern.isError()) {
                return ParseResult.error(pattern);
            }
            return ParseResult.ok(LikePredicate.of(LikeMode.LIKE, lhs, pattern.value(), negated));
        }

        @Override
        public ParseResult<? extends LikePredicate> parse(Cursor cur, ParseContext ctx) {
            return ParseResult.error("Unexpected LIKE parse", -1);
        }

        @Override
        public Class<LikePredicate> targetType() {
            return LikePredicate.class;
        }
    }

    private static final class IsNullPredicateParser implements Parser<IsNullPredicate>, InfixParser<Expression, IsNullPredicate> {
        @Override
        public ParseResult<IsNullPredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
            cur.expect("Expected IS", TokenType.IS);
            if (cur.match(TokenType.NOT)) {
                cur.advance();
                cur.expect("Expected NULL", TokenType.NULL);
                return ParseResult.ok(IsNullPredicate.of(lhs, true));
            }
            cur.expect("Expected NULL", TokenType.NULL);
            return ParseResult.ok(IsNullPredicate.of(lhs, false));
        }

        @Override
        public ParseResult<? extends IsNullPredicate> parse(Cursor cur, ParseContext ctx) {
            return ParseResult.error("Unexpected IS NULL parse", -1);
        }

        @Override
        public Class<IsNullPredicate> targetType() {
            return IsNullPredicate.class;
        }
    }

    private static final class IsDistinctFromPredicateParser implements Parser<IsDistinctFromPredicate>, InfixParser<Expression, IsDistinctFromPredicate> {
        @Override
        public ParseResult<IsDistinctFromPredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
            cur.expect("Expected IS", TokenType.IS);
            boolean negated = false;
            if (cur.match(TokenType.NOT)) {
                cur.advance();
                negated = true;
            }
            cur.expect("Expected DISTINCT", TokenType.DISTINCT);
            cur.expect("Expected FROM", TokenType.FROM);
            var rhs = ctx.parse(Expression.class, cur);
            if (rhs.isError()) {
                return ParseResult.error(rhs);
            }
            return ParseResult.ok(IsDistinctFromPredicate.of(lhs, rhs.value(), negated));
        }

        @Override
        public ParseResult<? extends IsDistinctFromPredicate> parse(Cursor cur, ParseContext ctx) {
            return ParseResult.error("Unexpected IS DISTINCT parse", -1);
        }

        @Override
        public Class<IsDistinctFromPredicate> targetType() {
            return IsDistinctFromPredicate.class;
        }
    }

    private static final class RegexPredicateParser implements Parser<RegexPredicate>, InfixParser<Expression, RegexPredicate> {
        @Override
        public ParseResult<RegexPredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
            cur.expect("Expected regex operator", TokenType.OPERATOR);
            var rhs = ctx.parse(Expression.class, cur);
            if (rhs.isError()) {
                return ParseResult.error(rhs);
            }
            return ParseResult.ok(RegexPredicate.of(RegexMode.MATCH, lhs, rhs.value(), false));
        }

        @Override
        public ParseResult<? extends RegexPredicate> parse(Cursor cur, ParseContext ctx) {
            return ParseResult.error("Unexpected regex parse", -1);
        }

        @Override
        public Class<RegexPredicate> targetType() {
            return RegexPredicate.class;
        }
    }

    private static final class UnaryPredicateParser implements Parser<UnaryPredicate>, InfixParser<Expression, UnaryPredicate> {
        @Override
        public ParseResult<UnaryPredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
            if (cur.match(TokenType.IS)) {
                cur.advance();
                if (cur.match(TokenType.NOT)) {
                    cur.advance();
                }
                if (cur.matchAny(TokenType.TRUE, TokenType.FALSE, TokenType.IDENT)) {
                    cur.advance();
                }
            }
            return ParseResult.ok(UnaryPredicate.of(lhs));
        }

        @Override
        public ParseResult<? extends UnaryPredicate> parse(Cursor cur, ParseContext ctx) {
            return ParseResult.error("Unexpected unary parse", -1);
        }

        @Override
        public Class<UnaryPredicate> targetType() {
            return UnaryPredicate.class;
        }
    }
}
