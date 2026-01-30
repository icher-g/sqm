package io.sqm.parser;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParamExprParserTest {

    @Test
    void parsesAnonymousParam() {
        var ctx = contextWithParamParsers();
        var result = ctx.parse(ParamExpr.class, "anon");

        assertTrue(result.ok());
        assertInstanceOf(AnonymousParamExpr.class, result.value());
    }

    @Test
    void parsesNamedParam() {
        var ctx = contextWithParamParsers();
        var result = ctx.parse(ParamExpr.class, "named");

        assertTrue(result.ok());
        var param = assertInstanceOf(NamedParamExpr.class, result.value());
        assertEquals("named", param.name());
    }

    @Test
    void parsesOrdinalParam() {
        var ctx = contextWithParamParsers();
        var result = ctx.parse(ParamExpr.class, "ord");

        assertTrue(result.ok());
        var param = assertInstanceOf(OrdinalParamExpr.class, result.value());
        assertEquals(7, param.index());
    }

    @Test
    void errorsWhenNoParamParserMatches() {
        var ctx = contextWithParamParsers();
        var result = ctx.parse(ParamExpr.class, "other");

        assertTrue(result.isError());
    }

    private static ParseContext contextWithParamParsers() {
        var repo = new DefaultParsersRepository()
            .register(ParamExpr.class, new ParamExprParser())
            .register(AnonymousParamExpr.class, new AnonymousParamParser())
            .register(NamedParamExpr.class, new NamedParamParser())
            .register(OrdinalParamExpr.class, new OrdinalParamParser());
        return TestSupport.context(repo);
    }

    private static final class AnonymousParamParser implements MatchableParser<AnonymousParamExpr> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.IDENT) && "anon".equals(cur.peek().lexeme());
        }

        @Override
        public ParseResult<? extends AnonymousParamExpr> parse(Cursor cur, ParseContext ctx) {
            cur.advance();
            return ParseResult.ok(AnonymousParamExpr.of());
        }

        @Override
        public Class<AnonymousParamExpr> targetType() {
            return AnonymousParamExpr.class;
        }
    }

    private static final class NamedParamParser implements MatchableParser<NamedParamExpr> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.IDENT) && "named".equals(cur.peek().lexeme());
        }

        @Override
        public ParseResult<? extends NamedParamExpr> parse(Cursor cur, ParseContext ctx) {
            var token = cur.expect("Expected name", TokenType.IDENT);
            return ParseResult.ok(NamedParamExpr.of(token.lexeme()));
        }

        @Override
        public Class<NamedParamExpr> targetType() {
            return NamedParamExpr.class;
        }
    }

    private static final class OrdinalParamParser implements MatchableParser<OrdinalParamExpr> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.IDENT) && "ord".equals(cur.peek().lexeme());
        }

        @Override
        public ParseResult<? extends OrdinalParamExpr> parse(Cursor cur, ParseContext ctx) {
            cur.advance();
            return ParseResult.ok(OrdinalParamExpr.of(7));
        }

        @Override
        public Class<OrdinalParamExpr> targetType() {
            return OrdinalParamExpr.class;
        }
    }
}
