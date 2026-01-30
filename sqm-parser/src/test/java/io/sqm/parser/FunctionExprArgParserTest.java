package io.sqm.parser;

import io.sqm.core.ColumnExpr;
import io.sqm.core.Expression;
import io.sqm.core.FunctionExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;
import org.junit.jupiter.api.Test;

import static io.sqm.parser.core.OperatorTokens.isStar;
import static org.junit.jupiter.api.Assertions.*;

class FunctionExprArgParserTest {

    @Test
    void parsesStarArgFirst() {
        var ctx = contextWithArgParsers();

        var result = ctx.parse(FunctionExpr.Arg.class, "*");

        assertTrue(result.ok());
        assertInstanceOf(FunctionExpr.Arg.StarArg.class, result.value());
    }

    @Test
    void fallsBackToExpressionArg() {
        var ctx = contextWithArgParsers();

        var result = ctx.parse(FunctionExpr.Arg.class, "col");

        assertTrue(result.ok());
        var arg = assertInstanceOf(FunctionExpr.Arg.ExprArg.class, result.value());
        assertInstanceOf(ColumnExpr.class, arg.expr());
    }

    private static ParseContext contextWithArgParsers() {
        var repo = new DefaultParsersRepository()
            .register(FunctionExpr.Arg.class, new FunctionExprArgParser())
            .register(FunctionExpr.Arg.StarArg.class, new StarArgParser())
            .register(FunctionExpr.Arg.ExprArg.class, new ExprArgParser())
            .register(Expression.class, new SimpleExpressionParser());
        return TestSupport.context(repo);
    }

    private static final class StarArgParser implements MatchableParser<FunctionExpr.Arg.StarArg> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(t -> isStar(t));
        }

        @Override
        public ParseResult<? extends FunctionExpr.Arg.StarArg> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected *", t -> isStar(t));
            return ParseResult.ok(FunctionExpr.Arg.star());
        }

        @Override
        public Class<FunctionExpr.Arg.StarArg> targetType() {
            return FunctionExpr.Arg.StarArg.class;
        }
    }

    private static final class ExprArgParser implements Parser<FunctionExpr.Arg.ExprArg> {
        @Override
        public ParseResult<? extends FunctionExpr.Arg.ExprArg> parse(Cursor cur, ParseContext ctx) {
            var expr = ctx.parse(Expression.class, cur);
            if (expr.isError()) {
                return ParseResult.error(expr);
            }
            return ParseResult.ok(FunctionExpr.Arg.expr(expr.value()));
        }

        @Override
        public Class<FunctionExpr.Arg.ExprArg> targetType() {
            return FunctionExpr.Arg.ExprArg.class;
        }
    }

    private static final class SimpleExpressionParser implements Parser<Expression> {
        @Override
        public ParseResult<? extends Expression> parse(Cursor cur, ParseContext ctx) {
            var token = cur.expect("Expected identifier", io.sqm.parser.core.TokenType.IDENT);
            return ParseResult.ok(Expression.column(token.lexeme()));
        }

        @Override
        public Class<Expression> targetType() {
            return Expression.class;
        }
    }
}
