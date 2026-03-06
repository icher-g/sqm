package io.sqm.parser;

import io.sqm.core.AnyAllPredicate;
import io.sqm.core.BetweenPredicate;
import io.sqm.core.ComparisonOperator;
import io.sqm.core.Expression;
import io.sqm.core.InPredicate;
import io.sqm.core.IsDistinctFromPredicate;
import io.sqm.core.IsNullPredicate;
import io.sqm.core.LikeMode;
import io.sqm.core.LikePredicate;
import io.sqm.core.Predicate;
import io.sqm.core.Quantifier;
import io.sqm.core.Query;
import io.sqm.core.RegexMode;
import io.sqm.core.RegexPredicate;
import io.sqm.core.RowExpr;
import io.sqm.core.UnaryPredicate;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.col;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtomicPredicateParserRegexKeywordTest {

    @Test
    void parsesRegexKeywordWithoutNot() {
        var result = context().parse(Predicate.class, "a REGEXP b");

        assertTrue(result.ok());
        assertInstanceOf(RegexPredicate.class, result.value());
    }

    @Test
    void parsesRegexKeywordWithNot() {
        var result = context().parse(Predicate.class, "a NOT RLIKE b");

        assertTrue(result.ok());
        var predicate = assertInstanceOf(RegexPredicate.class, result.value());
        assertTrue(predicate.negated());
    }

    @Test
    void parsesNotIlikeBranch() {
        var result = context().parse(Predicate.class, "a NOT ILIKE b");

        assertTrue(result.ok());
        var predicate = assertInstanceOf(LikePredicate.class, result.value());
        assertTrue(predicate.negated());
    }

    @Test
    void parsesIsNotDistinctBranch() {
        var result = context().parse(Predicate.class, "a IS NOT DISTINCT FROM b");

        assertTrue(result.ok());
        var predicate = assertInstanceOf(IsDistinctFromPredicate.class, result.value());
        assertTrue(predicate.negated());
    }

    private static ParseContext context() {
        var repo = new DefaultParsersRepository()
            .register(Predicate.class, new Wrapper())
            .register(Expression.class, new ExprParser())
            .register(AnyAllPredicate.class, new AnyAllParser())
            .register(io.sqm.core.ComparisonPredicate.class, new ComparisonParser())
            .register(BetweenPredicate.class, new BetweenParser())
            .register(InPredicate.class, new InParser())
            .register(LikePredicate.class, new LikeParser())
            .register(IsNullPredicate.class, new IsNullParser())
            .register(IsDistinctFromPredicate.class, new IsDistinctParser())
            .register(RegexPredicate.class, new RegexParser())
            .register(UnaryPredicate.class, new UnaryParser());
        return TestSupport.context(repo);
    }

    private static final class Wrapper implements Parser<Predicate> {
        @Override
        public ParseResult<? extends Predicate> parse(Cursor cur, ParseContext ctx) {
            return new AtomicPredicateParser().parse(cur, ctx);
        }

        @Override
        public Class<Predicate> targetType() {
            return Predicate.class;
        }
    }

    private static final class ExprParser implements Parser<Expression> {
        @Override
        public ParseResult<? extends Expression> parse(Cursor cur, ParseContext ctx) {
            var token = cur.expect("Expected identifier", TokenType.IDENT);
            return ParseResult.ok(col(token.lexeme()));
        }

        @Override
        public Class<Expression> targetType() {
            return Expression.class;
        }
    }

    private static final class ComparisonParser implements Parser<io.sqm.core.ComparisonPredicate>, InfixParser<Expression, io.sqm.core.ComparisonPredicate> {
        @Override
        public ParseResult<io.sqm.core.ComparisonPredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
            cur.expect("Expected comparison operator", TokenType.OPERATOR);
            var rhs = ctx.parse(Expression.class, cur);
            if (rhs.isError()) {
                return ParseResult.error(rhs);
            }
            return ParseResult.ok(io.sqm.core.ComparisonPredicate.of(lhs, ComparisonOperator.EQ, rhs.value()));
        }

        @Override
        public ParseResult<? extends io.sqm.core.ComparisonPredicate> parse(Cursor cur, ParseContext ctx) {
            return ParseResult.error("Unexpected comparison parse", -1);
        }

        @Override
        public Class<io.sqm.core.ComparisonPredicate> targetType() {
            return io.sqm.core.ComparisonPredicate.class;
        }
    }

    private static final class AnyAllParser implements Parser<AnyAllPredicate>, InfixParser<Expression, AnyAllPredicate> {
        @Override
        public ParseResult<AnyAllPredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
            cur.expect("Expected comparison operator", TokenType.OPERATOR);
            cur.advance();
            cur.expect("Expected marker", TokenType.IDENT);
            return ParseResult.ok(AnyAllPredicate.of(lhs, ComparisonOperator.EQ, Query.select(Expression.literal(1)).build(), Quantifier.ANY));
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

    private static final class BetweenParser implements Parser<BetweenPredicate>, InfixParser<Expression, BetweenPredicate> {
        @Override
        public ParseResult<BetweenPredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
            return ParseResult.error("unused", -1);
        }

        @Override
        public ParseResult<? extends BetweenPredicate> parse(Cursor cur, ParseContext ctx) {
            return ParseResult.error("unused", -1);
        }

        @Override
        public Class<BetweenPredicate> targetType() {
            return BetweenPredicate.class;
        }
    }

    private static final class InParser implements Parser<InPredicate>, InfixParser<Expression, InPredicate> {
        @Override
        public ParseResult<InPredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
            boolean negated = cur.consumeIf(TokenType.NOT);
            cur.expect("Expected IN", TokenType.IN);
            cur.expect("Expected (", TokenType.LPAREN);
            var value = cur.expect("Expected value", TokenType.IDENT).lexeme();
            cur.expect("Expected )", TokenType.RPAREN);
            return ParseResult.ok(InPredicate.of(lhs, RowExpr.of(List.of(col(value))), negated));
        }

        @Override
        public ParseResult<? extends InPredicate> parse(Cursor cur, ParseContext ctx) {
            return ParseResult.error("unused", -1);
        }

        @Override
        public Class<InPredicate> targetType() {
            return InPredicate.class;
        }
    }

    private static final class LikeParser implements Parser<LikePredicate>, InfixParser<Expression, LikePredicate> {
        @Override
        public ParseResult<LikePredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
            boolean negated = cur.consumeIf(TokenType.NOT);
            if (cur.matchAny(TokenType.LIKE, TokenType.ILIKE, TokenType.SIMILAR)) {
                cur.advance();
            }
            var rhs = ctx.parse(Expression.class, cur);
            if (rhs.isError()) {
                return ParseResult.error(rhs);
            }
            return ParseResult.ok(LikePredicate.of(LikeMode.LIKE, lhs, rhs.value(), negated));
        }

        @Override
        public ParseResult<? extends LikePredicate> parse(Cursor cur, ParseContext ctx) {
            return ParseResult.error("unused", -1);
        }

        @Override
        public Class<LikePredicate> targetType() {
            return LikePredicate.class;
        }
    }

    private static final class IsNullParser implements Parser<IsNullPredicate>, InfixParser<Expression, IsNullPredicate> {
        @Override
        public ParseResult<IsNullPredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
            return ParseResult.error("unused", -1);
        }

        @Override
        public ParseResult<? extends IsNullPredicate> parse(Cursor cur, ParseContext ctx) {
            return ParseResult.error("unused", -1);
        }

        @Override
        public Class<IsNullPredicate> targetType() {
            return IsNullPredicate.class;
        }
    }

    private static final class IsDistinctParser implements Parser<IsDistinctFromPredicate>, InfixParser<Expression, IsDistinctFromPredicate> {
        @Override
        public ParseResult<IsDistinctFromPredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
            cur.expect("Expected IS", TokenType.IS);
            boolean negated = cur.consumeIf(TokenType.NOT);
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
            return ParseResult.error("unused", -1);
        }

        @Override
        public Class<IsDistinctFromPredicate> targetType() {
            return IsDistinctFromPredicate.class;
        }
    }

    private static final class RegexParser implements Parser<RegexPredicate>, InfixParser<Expression, RegexPredicate> {
        @Override
        public ParseResult<RegexPredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
            boolean negated = cur.consumeIf(TokenType.NOT);
            if (cur.match(TokenType.OPERATOR)) {
                cur.advance();
            } else {
                cur.expect("Expected regex keyword", TokenType.IDENT);
            }
            var rhs = ctx.parse(Expression.class, cur);
            if (rhs.isError()) {
                return ParseResult.error(rhs);
            }
            return ParseResult.ok(RegexPredicate.of(RegexMode.MATCH, lhs, rhs.value(), negated));
        }

        @Override
        public ParseResult<? extends RegexPredicate> parse(Cursor cur, ParseContext ctx) {
            return ParseResult.error("unused", -1);
        }

        @Override
        public Class<RegexPredicate> targetType() {
            return RegexPredicate.class;
        }
    }

    private static final class UnaryParser implements Parser<UnaryPredicate>, InfixParser<Expression, UnaryPredicate> {
        @Override
        public ParseResult<UnaryPredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
            return ParseResult.ok(UnaryPredicate.of(lhs));
        }

        @Override
        public ParseResult<? extends UnaryPredicate> parse(Cursor cur, ParseContext ctx) {
            return ParseResult.error("unused", -1);
        }

        @Override
        public Class<UnaryPredicate> targetType() {
            return UnaryPredicate.class;
        }
    }
}
