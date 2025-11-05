package io.sqm.parser.ansi;

import io.sqm.core.ColumnExpr;
import io.sqm.core.GroupItem;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class GroupItemParser implements Parser<GroupItem> {

    private static boolean isPositiveInteger(String s) {
        // Fast path: all digits, no sign, no decimal.
        for (int i = 0, n = s.length(); i < n; i++) {
            char ch = s.charAt(i);
            if (ch < '0' || ch > '9') return false;
        }
        return !s.isEmpty();
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<GroupItem> parse(Cursor cur, ParseContext ctx) {
        // Positional GROUP BY: "1", "2", ...
        // SQL allows positive 1-based ordinals.
        if (isPositiveInteger(cur.peek().lexeme())) {
            int pos = Integer.parseInt(cur.advance().lexeme());
            if (pos <= 0) {
                return error("GROUP BY position must be a positive integer", pos);
            }
            return finalize(cur, ctx, GroupItem.of(pos));
        }

        // Otherwise: delegate to the column parser
        var result = ctx.parse(ColumnExpr.class, cur);
        if (result.isError()) {
            return error(result);
        }
        return finalize(cur, ctx, GroupItem.of(result.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<GroupItem> targetType() {
        return GroupItem.class;
    }
}
