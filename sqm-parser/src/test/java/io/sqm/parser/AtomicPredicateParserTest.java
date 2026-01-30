package io.sqm.parser;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;
import org.junit.jupiter.api.Test;

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
    void parsesComparisonPredicate() {
        var ctx = contextWithPredicateParsers();
        var result = ctx.parse(Predicate.class, "a = b");

        assertTrue(result.ok());
        assertInstanceOf(ComparisonPredicate.class, result.value());
    }

    @Test
    void parsesIsNullPredicate() {
        var ctx = contextWithPredicateParsers();
        var result = ctx.parse(Predicate.class, "a IS NULL");

        assertTrue(result.ok());
        assertInstanceOf(IsNullPredicate.class, result.value());
    }

    @Test
    void parsesRegexPredicate() {
        var ctx = contextWithPredicateParsers();
        var result = ctx.parse(Predicate.class, "a ~ b");

        assertTrue(result.ok());
        assertInstanceOf(RegexPredicate.class, result.value());
    }

    @Test
    void fallsBackToUnaryPredicate() {
        var ctx = contextWithPredicateParsers();
        var result = ctx.parse(Predicate.class, "a");

        assertTrue(result.ok());
        assertInstanceOf(UnaryPredicate.class, result.value());
    }

    private static ParseContext contextWithPredicateParsers() {
        var repo = new DefaultParsersRepository()
            .register(Predicate.class, new AtomicPredicateWrapper())
            .register(Expression.class, new ExpressionStubParser())
            .register(ExistsPredicate.class, new ExistsPredicateParser())
            .register(NotPredicate.class, new NotPredicateParser())
            .register(ComparisonPredicate.class, new ComparisonPredicateParser())
            .register(IsNullPredicate.class, new IsNullPredicateParser())
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
            return ParseResult.ok(ExistsPredicate.of(Query.select(Expression.literal(1)), false));
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
            return ParseResult.ok(NotPredicate.of(UnaryPredicate.of(Expression.literal(1))));
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
