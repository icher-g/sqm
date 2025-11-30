package io.sqm.parser;

import io.sqm.core.ExprSelectItem;
import io.sqm.core.QualifiedStarSelectItem;
import io.sqm.core.SelectItem;
import io.sqm.core.StarSelectItem;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.MatchResult;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class SelectItemParser implements Parser<SelectItem> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends SelectItem> parse(Cursor cur, ParseContext ctx) {
        MatchResult<? extends SelectItem> matched = ctx.parseIfMatch(StarSelectItem.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        matched = ctx.parseIfMatch(QualifiedStarSelectItem.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        // try to parse expression.
        return ctx.parse(ExprSelectItem.class, cur);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<SelectItem> targetType() {
        return SelectItem.class;
    }
}
