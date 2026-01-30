package io.sqm.parser;

import io.sqm.core.CompositeQuery;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.WithQuery;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryParserTest {

    @Test
    void delegatesToWithQueryWhenWithTokenPresent() {
        var ctx = contextWithQueryParsers();
        var result = ctx.parse(Query.class, "WITH");

        assertTrue(result.ok());
        assertInstanceOf(WithQuery.class, result.value());
    }

    @Test
    void delegatesToCompositeQueryOtherwise() {
        var ctx = contextWithQueryParsers();
        var result = ctx.parse(Query.class, "SELECT");

        assertTrue(result.ok());
        assertInstanceOf(CompositeQuery.class, result.value());
    }

    private static ParseContext contextWithQueryParsers() {
        var repo = new DefaultParsersRepository()
            .register(Query.class, new io.sqm.parser.QueryParser())
            .register(WithQuery.class, new WithQueryParser())
            .register(CompositeQuery.class, new CompositeQueryParser());
        return TestSupport.context(repo);
    }

    private static final class WithQueryParser implements io.sqm.parser.spi.Parser<WithQuery> {
        @Override
        public ParseResult<? extends WithQuery> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected WITH", TokenType.WITH);
            return ParseResult.ok(WithQuery.of());
        }

        @Override
        public Class<WithQuery> targetType() {
            return WithQuery.class;
        }
    }

    private static final class CompositeQueryParser implements io.sqm.parser.spi.Parser<CompositeQuery> {
        @Override
        public ParseResult<? extends CompositeQuery> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected SELECT", TokenType.SELECT);
            return ParseResult.ok(CompositeQuery.of(List.of(SelectQuery.of()), List.of()));
        }

        @Override
        public Class<CompositeQuery> targetType() {
            return CompositeQuery.class;
        }
    }
}
