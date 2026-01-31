package io.sqm.parser;

import io.sqm.core.GroupItem;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.MatchResult;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Delegating parser for {@link GroupItem} elements.
 */
public class GroupItemParser implements Parser<GroupItem> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a cursor containing tokens to parse
     * @param ctx a parser context containing parsers and lookups
     * @return a parsing result
     */
    @Override
    public ParseResult<? extends GroupItem> parse(Cursor cur, ParseContext ctx) {
        MatchResult<? extends GroupItem> matched = ctx.parseIfMatch(GroupItem.GroupingSets.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        matched = ctx.parseIfMatch(GroupItem.Rollup.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        matched = ctx.parseIfMatch(GroupItem.Cube.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        matched = ctx.parseIfMatch(GroupItem.GroupingSet.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        return ctx.parse(GroupItem.SimpleGroupItem.class, cur);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler
     */
    @Override
    public Class<GroupItem> targetType() {
        return GroupItem.class;
    }
}
