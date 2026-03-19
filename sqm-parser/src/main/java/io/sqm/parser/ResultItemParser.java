package io.sqm.parser;

import io.sqm.core.ExprResultItem;
import io.sqm.core.OutputStarResultItem;
import io.sqm.core.QualifiedStarResultItem;
import io.sqm.core.ResultItem;
import io.sqm.core.StarResultItem;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.MatchResult;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Parser for RESULT-list items.
 */
public class ResultItemParser implements Parser<ResultItem> {
    /**
     * Creates a result-item parser.
     */
    public ResultItemParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends ResultItem> parse(Cursor cur, ParseContext ctx) {
        MatchResult<? extends ResultItem> matched = ctx.parseIfMatch(StarResultItem.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        matched = ctx.parseIfMatch(OutputStarResultItem.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        matched = ctx.parseIfMatch(QualifiedStarResultItem.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        // try to parse expression.
        return ctx.parse(ExprResultItem.class, cur);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<ResultItem> targetType() {
        return ResultItem.class;
    }
}
