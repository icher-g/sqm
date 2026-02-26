package io.sqm.parser;

import io.sqm.core.CompositeQuery;
import io.sqm.core.Query;
import io.sqm.core.WithQuery;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Entry-point parser for SQL query nodes.
 */
public class QueryParser implements Parser<Query> {
    /**
     * Creates a query parser.
     */
    public QueryParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends Query> parse(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.WITH)) {
            return ctx.parse(WithQuery.class, cur);
        }
        return ctx.parse(CompositeQuery.class, cur);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<Query> targetType() {
        return Query.class;
    }
}
