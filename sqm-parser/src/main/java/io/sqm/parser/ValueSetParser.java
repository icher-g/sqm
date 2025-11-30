package io.sqm.parser;

import io.sqm.core.QueryExpr;
import io.sqm.core.RowExpr;
import io.sqm.core.RowListExpr;
import io.sqm.core.ValueSet;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.MatchResult;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class ValueSetParser implements Parser<ValueSet> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends ValueSet> parse(Cursor cur, ParseContext ctx) {
        MatchResult<? extends ValueSet> matched = ctx.parseIfMatch(QueryExpr.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        matched = ctx.parseIfMatch(RowListExpr.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        return ctx.parse(RowExpr.class, cur);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<ValueSet> targetType() {
        return ValueSet.class;
    }
}
