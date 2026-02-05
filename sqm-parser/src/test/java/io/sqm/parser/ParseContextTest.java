package io.sqm.parser;

import io.sqm.core.ExprSelectItem;
import io.sqm.core.Expression;
import io.sqm.core.SelectItem;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.Token;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParseContextTest {

    @Test
    void parseRejectsBlankSpec() {
        var ctx = TestSupport.context(new DefaultParsersRepository());

        assertTrue(ctx.parse(Expression.class, " ").isError());
        assertTrue(ctx.parse(Expression.class, "\t").isError());
    }

    @Test
    void parseErrorsWhenParserMissing() {
        var ctx = TestSupport.context(new DefaultParsersRepository());

        assertTrue(ctx.parse(Expression.class, "x").isError());
    }

    @Test
    void parseFinalizesAndDetectsTrailingTokens() {
        var repo = new DefaultParsersRepository();
        repo.register(Expression.class, new SingleTokenExpressionParser());
        var ctx = TestSupport.context(repo);

        var result = ctx.parse(Expression.class, "a b");
        assertTrue(result.isError());
    }

    @Test
    void parseIfMatchReturnsMatchedErrorForNonMatchableParser() {
        var repo = new DefaultParsersRepository();
        repo.register(Expression.class, new SingleTokenExpressionParser());
        var ctx = TestSupport.context(repo);

        var cur = Cursor.of("x", ctx.identifierQuoting());
        var match = ctx.parseIfMatch(Expression.class, cur);

        assertTrue(match.match());
        assertTrue(match.result().isError());
    }

    @Test
    void parseIfMatchReturnsNotMatchedWithoutConsuming() {
        var repo = new DefaultParsersRepository();
        repo.register(Expression.class, new NeverMatchExpressionParser());
        var ctx = TestSupport.context(repo);

        var cur = Cursor.of("x", ctx.identifierQuoting());
        var before = cur.peek().lexeme();
        var match = ctx.parseIfMatch(Expression.class, cur);

        assertFalse(match.match());
        assertEquals(before, cur.peek().lexeme());
    }

    @Test
    void parseIfMatchInfixSupportsMatchableInfixParser() {
        var repo = new DefaultParsersRepository();
        repo.register(Expression.class, new InfixExpressionParser());
        var ctx = TestSupport.context(repo);

        var cur = Cursor.of("x", ctx.identifierQuoting());
        var lhs = Expression.literal(1);
        var match = ctx.parseIfMatch(Expression.class, lhs, cur);

        assertTrue(match.match());
        assertTrue(match.result().ok());
        assertTrue(cur.isEof());
    }

    @Test
    void parseInfixErrorsWhenParserDoesNotSupportInfix() {
        var repo = new DefaultParsersRepository();
        repo.register(Expression.class, new SingleTokenExpressionParser());
        var ctx = TestSupport.context(repo);

        var cur = Cursor.of("x", ctx.identifierQuoting());
        var result = ctx.parse(Expression.class, Expression.literal(1), cur);

        assertTrue(result.isError());
    }

    @Test
    void parseEnclosedParsesParenthesizedExpression() {
        var repo = new DefaultParsersRepository();
        repo.register(Expression.class, new SingleTokenExpressionParser());
        repo.register(SelectItem.class, new EnclosedSelectItemParser());
        var ctx = TestSupport.context(repo);

        var result = ctx.parse(SelectItem.class, "(x)");
        assertTrue(result.ok());
        assertInstanceOf(ExprSelectItem.class, result.value());
    }

    @Test
    void operatorPolicy_is_available_from_context() {
        var ctx = TestSupport.context(new DefaultParsersRepository());

        var generic = ctx.operatorPolicy().isGenericBinaryOperator(new Token(TokenType.OPERATOR, "||", 0));
        var arithmetic = ctx.operatorPolicy().isGenericBinaryOperator(new Token(TokenType.OPERATOR, "+", 0));

        assertTrue(generic);
        assertFalse(arithmetic);
    }

    private static final class SingleTokenExpressionParser implements Parser<Expression> {
        @Override
        public ParseResult<? extends Expression> parse(Cursor cur, ParseContext ctx) {
            cur.advance();
            return ParseResult.ok(Expression.literal(1));
        }

        @Override
        public Class<Expression> targetType() {
            return Expression.class;
        }
    }

    private static final class NeverMatchExpressionParser implements MatchableParser<Expression> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return false;
        }

        @Override
        public ParseResult<? extends Expression> parse(Cursor cur, ParseContext ctx) {
            throw new AssertionError("parse must not be called when match is false");
        }

        @Override
        public Class<Expression> targetType() {
            return Expression.class;
        }
    }

    private static final class InfixExpressionParser implements MatchableParser<Expression>, InfixParser<Expression, Expression> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.IDENT);
        }

        @Override
        public ParseResult<? extends Expression> parse(Cursor cur, ParseContext ctx) {
            throw new AssertionError("Direct parse not used in this test");
        }

        @Override
        public ParseResult<Expression> parse(Expression lhs, Cursor cur, ParseContext ctx) {
            cur.advance();
            return ParseResult.ok(Expression.literal(2));
        }

        @Override
        public Class<Expression> targetType() {
            return Expression.class;
        }
    }

    private static final class EnclosedSelectItemParser implements Parser<SelectItem> {
        @Override
        public ParseResult<? extends SelectItem> parse(Cursor cur, ParseContext ctx) {
            var expr = ctx.parseEnclosed(Expression.class, cur);
            if (expr.isError()) {
                return ParseResult.error(expr);
            }
            return ParseResult.ok(ExprSelectItem.of(expr.value()));
        }

        @Override
        public Class<SelectItem> targetType() {
            return SelectItem.class;
        }
    }
}
