package io.sqm.parser;

import io.sqm.core.Expression;
import io.sqm.core.Identifier;
import io.sqm.core.QuoteStyle;
import io.sqm.core.utils.Pair;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.ParserException;
import io.sqm.parser.core.Token;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static io.sqm.dsl.Dsl.col;
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
        assertEquals("alias", parser.parseAliasIdentifier(withAs).value());
        assertTrue(withAs.isEof());

        var withoutAs = Cursor.of("alias", quoting);
        assertEquals("alias", parser.parseAliasIdentifier(withoutAs).value());
        assertTrue(withoutAs.isEof());
    }

    @Test
    void parseAliasReturnsNullWhenNotPresent() {
        var parser = new DummyParser();
        var quoting = TestSupport.context(new DefaultParsersRepository()).identifierQuoting();
        var cur = Cursor.of("1", quoting);

        assertNull(parser.parseAliasIdentifier(cur));
        assertTrue(cur.match(TokenType.NUMBER));
    }

    @Test
    void parseAliasIdentifierFailsWhenAsHasNoIdentifier() {
        var parser = new DummyParser();
        var quoting = TestSupport.context(new DefaultParsersRepository()).identifierQuoting();
        var cur = Cursor.of("AS 1", quoting);

        var error = assertThrows(ParserException.class, () -> parser.parseAliasIdentifier(cur));
        assertTrue(error.getMessage().contains("Expected alias after AS"));
    }

    @Test
    void parseColumnAliasesParsesList() {
        var parser = new DummyParser();
        var quoting = TestSupport.context(new DefaultParsersRepository()).identifierQuoting();
        var cur = Cursor.of("t(c1, c2)", quoting);

        Pair<Identifier, List<Identifier>> result = parser.parseColumnAliasIdentifiers(cur);

        assertEquals("t", result.first().value());
        assertEquals(List.of("c1", "c2"), result.second().stream().map(v -> v.value()).toList());
        assertTrue(cur.isEof());
    }

    @Test
    void toIdentifierPreservesQuoteStyle() {
        var parser = new DummyParser();

        assertEquals(QuoteStyle.DOUBLE_QUOTE, parser.toIdentifier(new Token(TokenType.IDENT, "X", 0, '"')).quoteStyle());
        assertEquals(QuoteStyle.BACKTICK, parser.toIdentifier(new Token(TokenType.IDENT, "X", 0, '`')).quoteStyle());
        assertEquals(QuoteStyle.BRACKETS, parser.toIdentifier(new Token(TokenType.IDENT, "X", 0, '[')).quoteStyle());
    }

    @Test
    void parseQualifiedNameParsesMultipartAndOverloadWithFirstPart() {
        var parser = new DummyParser();
        var quoting = TestSupport.context(new DefaultParsersRepository()).identifierQuoting();

        var cur = Cursor.of("\"S\".t", quoting);
        var qn = parser.parseQualifiedName(cur);
        assertEquals(List.of("S", "t"), qn.values());
        assertEquals(QuoteStyle.DOUBLE_QUOTE, qn.parts().getFirst().quoteStyle());
        assertTrue(cur.isEof());

        var cur2 = Cursor.of(".t2", quoting);
        var qn2 = parser.parseQualifiedName(Identifier.of("s"), cur2);
        assertEquals(List.of("s", "t2"), qn2.values());
        assertEquals(QuoteStyle.NONE, qn2.parts().get(1).quoteStyle());
        assertTrue(cur2.isEof());
    }

    @Test
    void parseQualifiedNameOverloadRejectsNullFirstPart() {
        var parser = new DummyParser();
        var quoting = TestSupport.context(new DefaultParsersRepository()).identifierQuoting();
        var cur = Cursor.of(".t", quoting);

        assertThrows(NullPointerException.class, () -> parser.parseQualifiedName(null, cur));
    }

    @Test
    void parseColumnAliasIdentifiersParsesAliasWithoutDerivedColumns() {
        var parser = new DummyParser();
        var quoting = TestSupport.context(new DefaultParsersRepository()).identifierQuoting();
        var cur = Cursor.of("\"t\"", quoting);

        Pair<Identifier, List<Identifier>> result = parser.parseColumnAliasIdentifiers(cur);

        assertEquals("t", result.first().value());
        assertEquals(QuoteStyle.DOUBLE_QUOTE, result.first().quoteStyle());
        assertNull(result.second());
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
            return ParseResult.ok(col(token.lexeme()));
        }

        @Override
        public Class<Expression> targetType() {
            return Expression.class;
        }
    }
}

