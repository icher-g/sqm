package io.sqm.parser;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.Test;

import static io.sqm.parser.core.OperatorTokens.isStar;
import static org.junit.jupiter.api.Assertions.*;

class SelectItemParserTest {

    @Test
    void parsesStarSelectItemFirst() {
        var ctx = contextWithSelectItemParsers();
        var result = ctx.parse(SelectItem.class, "*");

        assertTrue(result.ok());
        assertInstanceOf(StarSelectItem.class, result.value());
    }

    @Test
    void parsesQualifiedStarSelectItem() {
        var ctx = contextWithSelectItemParsers();
        var result = ctx.parse(SelectItem.class, "t.*");

        assertTrue(result.ok());
        var item = assertInstanceOf(QualifiedStarSelectItem.class, result.value());
        assertEquals("t", item.qualifier());
    }

    @Test
    void fallsBackToExpressionSelectItem() {
        var ctx = contextWithSelectItemParsers();
        var result = ctx.parse(SelectItem.class, "col");

        assertTrue(result.ok());
        var item = assertInstanceOf(ExprSelectItem.class, result.value());
        assertInstanceOf(ColumnExpr.class, item.expr());
    }

    private static ParseContext contextWithSelectItemParsers() {
        var repo = new DefaultParsersRepository()
            .register(SelectItem.class, new SelectItemParser())
            .register(StarSelectItem.class, new StarSelectItemParser())
            .register(QualifiedStarSelectItem.class, new QualifiedStarSelectItemParser())
            .register(ExprSelectItem.class, new ExprSelectItemParser())
            .register(Expression.class, new SimpleExpressionParser());
        return TestSupport.context(repo);
    }

    private static final class StarSelectItemParser implements MatchableParser<StarSelectItem> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(t -> isStar(t));
        }

        @Override
        public ParseResult<? extends StarSelectItem> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected *", t -> isStar(t));
            return ParseResult.ok(StarSelectItem.of());
        }

        @Override
        public Class<StarSelectItem> targetType() {
            return StarSelectItem.class;
        }
    }

    private static final class QualifiedStarSelectItemParser implements MatchableParser<QualifiedStarSelectItem> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.IDENT)
                && cur.match(TokenType.DOT, 1)
                && cur.match(t -> isStar(t), 2);
        }

        @Override
        public ParseResult<? extends QualifiedStarSelectItem> parse(Cursor cur, ParseContext ctx) {
            var qualifier = cur.expect("Expected qualifier", TokenType.IDENT).lexeme();
            cur.expect("Expected .", TokenType.DOT);
            cur.expect("Expected *", t -> isStar(t));
            return ParseResult.ok(QualifiedStarSelectItem.of(qualifier));
        }

        @Override
        public Class<QualifiedStarSelectItem> targetType() {
            return QualifiedStarSelectItem.class;
        }
    }

    private static final class ExprSelectItemParser implements io.sqm.parser.spi.Parser<ExprSelectItem> {
        @Override
        public ParseResult<? extends ExprSelectItem> parse(Cursor cur, ParseContext ctx) {
            var expr = ctx.parse(Expression.class, cur);
            if (expr.isError()) {
                return ParseResult.error(expr);
            }
            return ParseResult.ok(ExprSelectItem.of(expr.value()));
        }

        @Override
        public Class<ExprSelectItem> targetType() {
            return ExprSelectItem.class;
        }
    }

    private static final class SimpleExpressionParser implements io.sqm.parser.spi.Parser<Expression> {
        @Override
        public ParseResult<? extends Expression> parse(Cursor cur, ParseContext ctx) {
            var token = cur.expect("Expected identifier", TokenType.IDENT);
            return ParseResult.ok(Expression.column(token.lexeme()));
        }

        @Override
        public Class<Expression> targetType() {
            return Expression.class;
        }
    }
}
