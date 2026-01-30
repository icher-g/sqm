package io.sqm.parser;

import io.sqm.core.Expression;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;
import io.sqm.core.utils.Pair;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class ParserDefaultsTest {

    @Test
    void parseNumberHandlesLongDoubleAndFallback() {
        var parser = new DummyParser();

        assertEquals(10L, parser.parseNumber("10"));
        assertEquals(1.25d, parser.parseNumber("1.25"));
        assertEquals(1.0E3d, parser.parseNumber("1E3"));
        assertEquals("abc", parser.parseNumber("abc"));
    }

    @Test
    void parseAliasParsesAsKeywordAndIdentifier() {
        var parser = new DummyParser();
        var quoting = TestSupport.context(new DefaultParsersRepository()).identifierQuoting();

        var withAs = Cursor.of("AS alias", quoting);
        assertEquals("alias", parser.parseAlias(withAs));
        assertTrue(withAs.isEof());

        var withoutAs = Cursor.of("alias", quoting);
        assertEquals("alias", parser.parseAlias(withoutAs));
        assertTrue(withoutAs.isEof());
    }

    @Test
    void parseAliasReturnsNullWhenNotPresent() {
        var parser = new DummyParser();
        var quoting = TestSupport.context(new DefaultParsersRepository()).identifierQuoting();
        var cur = Cursor.of("1", quoting);

        assertNull(parser.parseAlias(cur));
        assertTrue(cur.match(TokenType.NUMBER));
    }

    @Test
    void parseColumnAliasesParsesList() {
        var parser = new DummyParser();
        var quoting = TestSupport.context(new DefaultParsersRepository()).identifierQuoting();
        var cur = Cursor.of("t(c1, c2)", quoting);

        Pair<String, List<String>> result = parser.parseColumnAliases(cur);

        assertEquals("t", result.first());
        assertEquals(List.of("c1", "c2"), result.second());
        assertTrue(cur.isEof());
    }

    @Test
    void parseItemsParsesCommaSeparatedExpressions() {
        var repo = new DefaultParsersRepository()
            .register(Expression.class, new IdentifierExpressionParser());
        var ctx = TestSupport.context(repo);
        ctx.callstack().push(Expression.class); // this is required for finalization not to look for EOF.
        var parser = new DummyParser();
        var cur = Cursor.of("a, b, c", ctx.identifierQuoting());

        var result = parser.parseItems(Expression.class, cur, ctx);

        assertTrue(result.ok());
        assertEquals(3, result.value().size());
        assertTrue(cur.isEof());
    }

    @Test
    void parseItemsPropagatesErrors() {
        var repo = new DefaultParsersRepository()
            .register(Expression.class, new IdentifierExpressionParser());
        var ctx = TestSupport.context(repo);
        ctx.callstack().push(Expression.class); // this is required for finalization not to look for EOF.
        var parser = new DummyParser();
        var cur = Cursor.of("a, bad", ctx.identifierQuoting());

        var result = parser.parseItems(Expression.class, cur, ctx);

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("bad expression"));
    }

    private static final class DummyParser implements Parser<Expression> {
        @Override
        public ParseResult<? extends Expression> parse(Cursor cur, ParseContext ctx) {
            return ParseResult.error("unused", -1);
        }

        @Override
        public Class<Expression> targetType() {
            return Expression.class;
        }
    }

    private static final class IdentifierExpressionParser implements Parser<Expression> {
        @Override
        public ParseResult<? extends Expression> parse(Cursor cur, ParseContext ctx) {
            var token = cur.expect("Expected identifier", TokenType.IDENT);
            if ("bad".equalsIgnoreCase(token.lexeme())) {
                return ParseResult.error("bad expression", token.pos());
            }
            return ParseResult.ok(Expression.column(token.lexeme()));
        }

        @Override
        public Class<Expression> targetType() {
            return Expression.class;
        }
    }
}
