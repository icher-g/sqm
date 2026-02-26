package io.sqm.parser.ansi;

import io.sqm.core.ColumnExpr;
import io.sqm.core.GroupItem;
import io.sqm.core.utils.Numbers;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * ANSI parser for simple GROUP BY items (expression or ordinal).
 */
public class SimpleGroupItemParser implements Parser<GroupItem.SimpleGroupItem> {
    /**
     * Creates a simple-group-item parser.
     */
    public SimpleGroupItemParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a cursor containing tokens to parse
     * @param ctx a parser context containing parsers and lookups
     * @return a parsing result
     */
    @Override
    public ParseResult<GroupItem.SimpleGroupItem> parse(Cursor cur, ParseContext ctx) {
        if (Numbers.isPositiveInteger(cur.peek().lexeme())) {
            int pos = Integer.parseInt(cur.advance().lexeme());
            if (pos <= 0) {
                return error("GROUP BY position must be a positive integer", pos);
            }
            return ok(GroupItem.of(pos));
        }

        var result = ctx.parse(ColumnExpr.class, cur);
        if (result.isError()) {
            return error(result);
        }
        return ok(GroupItem.of(result.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler
     */
    @Override
    public Class<GroupItem.SimpleGroupItem> targetType() {
        return GroupItem.SimpleGroupItem.class;
    }
}
