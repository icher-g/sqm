package io.sqm.parser;

import io.sqm.core.ExprSelectItem;
import io.sqm.core.QualifiedStarSelectItem;
import io.sqm.core.SelectItem;
import io.sqm.core.StarSelectItem;
import io.sqm.parser.core.Cursor;
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
    public ParseResult<SelectItem> parse(Cursor cur, ParseContext ctx) {
        if (ctx.lookups().looksLikeStar(cur)) {
            var res = ctx.parse(StarSelectItem.class, cur);
            return finalize(cur, ctx, res);
        }

        if (ctx.lookups().looksLikeQualifiedStar(cur)) {
            var res = ctx.parse(QualifiedStarSelectItem.class, cur);
            return finalize(cur, ctx, res);
        }

        var res = ctx.parse(ExprSelectItem.class, cur);
        return finalize(cur, ctx, res);
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
