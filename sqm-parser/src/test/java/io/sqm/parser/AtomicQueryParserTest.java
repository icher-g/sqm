package io.sqm.parser;

import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AtomicQueryParserTest {

    @Test
    void parsesSelectQuery() {
        var ctx = contextWithAtomicQueryParsers();
        var result = ctx.parse(Query.class, "SELECT");

        assertTrue(result.ok());
        assertInstanceOf(SelectQuery.class, result.value());
    }

    @Test
    void parsesParenthesizedQuery() {
        var ctx = contextWithAtomicQueryParsers();
        var result = ctx.parse(Query.class, "(SELECT)");

        assertTrue(result.ok());
        assertInstanceOf(SelectQuery.class, result.value());
    }

    @Test
    void errorsWhenNotStartingWithSelectOrParen() {
        var ctx = contextWithAtomicQueryParsers();
        var result = ctx.parse(Query.class, "FROM");

        assertTrue(result.isError());
    }

    private static ParseContext contextWithAtomicQueryParsers() {
        var repo = new DefaultParsersRepository()
            .register(Query.class, new AtomicQueryWrapper())
            .register(SelectQuery.class, new SelectQueryStubParser());
        return TestSupport.context(repo);
    }

    private static final class AtomicQueryWrapper implements io.sqm.parser.spi.Parser<Query> {
        @Override
        public ParseResult<? extends Query> parse(Cursor cur, ParseContext ctx) {
            return new AtomicQueryParser().parse(cur, ctx);
        }

        @Override
        public Class<Query> targetType() {
            return Query.class;
        }
    }

    private static final class SelectQueryStubParser implements io.sqm.parser.spi.Parser<SelectQuery> {
        @Override
        public ParseResult<? extends SelectQuery> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected SELECT", TokenType.SELECT);
            return ParseResult.ok(SelectQuery.of());
        }

        @Override
        public Class<SelectQuery> targetType() {
            return SelectQuery.class;
        }
    }
}
