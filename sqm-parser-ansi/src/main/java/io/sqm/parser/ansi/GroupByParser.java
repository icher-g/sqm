package io.sqm.parser.ansi;

import io.sqm.core.GroupBy;
import io.sqm.core.GroupItem;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;

public class GroupByParser implements Parser<GroupBy> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<GroupBy> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected GROUP", TokenType.GROUP);
        cur.expect("Expected BY after GROUP", TokenType.BY);
        List<GroupItem> items = new ArrayList<>();
        do {
            var gr = ctx.parse(GroupItem.class, cur);
            if (gr.isError()) {
                return error(gr);
            }
            items.add(gr.value());
        }
        while (cur.consumeIf(TokenType.COMMA));
        return finalize(cur, ctx, GroupBy.of(items));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<GroupBy> targetType() {
        return GroupBy.class;
    }
}
