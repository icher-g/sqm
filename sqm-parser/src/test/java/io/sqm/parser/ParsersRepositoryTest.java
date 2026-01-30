package io.sqm.parser;

import io.sqm.core.ColumnExpr;
import io.sqm.core.Expression;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;
import io.sqm.parser.spi.ParsersRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParsersRepositoryTest {

    @Test
    void registerHandlerUsesTargetType() {
        ParsersRepository repo = new DefaultParsersRepository();
        var parser = new ColumnExprParser();

        repo.register(parser);

        assertSame(parser, repo.get(ColumnExpr.class));
    }

    @Test
    void getForReturnsHandlerForInstance() {
        ParsersRepository repo = new DefaultParsersRepository();
        var parser = new ColumnExprParser();
        repo.register(parser);

        var handler = repo.getFor(ColumnExpr.of("a"));

        assertSame(parser, handler);
    }

    @Test
    void requireReturnsHandlerOrThrows() {
        ParsersRepository repo = new DefaultParsersRepository();
        var parser = new ColumnExprParser();
        repo.register(parser);

        assertSame(parser, repo.require(ColumnExpr.class));

        var ex = assertThrows(IllegalArgumentException.class, () -> repo.require(Expression.class));
        assertTrue(ex.getMessage().contains("No handler registered"));
    }

    @Test
    void requireForReturnsHandlerOrThrows() {
        ParsersRepository repo = new DefaultParsersRepository();
        var parser = new ColumnExprParser();
        repo.register(parser);

        assertSame(parser, repo.requireFor(ColumnExpr.of("b")));

        var ex = assertThrows(IllegalArgumentException.class, () -> repo.requireFor(Expression.literal("c")));
        assertTrue(ex.getMessage().contains("No handler registered"));
    }

    private static final class ColumnExprParser implements Parser<ColumnExpr> {
        @Override
        public ParseResult<? extends ColumnExpr> parse(Cursor cur, ParseContext ctx) {
            return ParseResult.ok(ColumnExpr.of("x"));
        }

        @Override
        public Class<ColumnExpr> targetType() {
            return ColumnExpr.class;
        }
    }
}
