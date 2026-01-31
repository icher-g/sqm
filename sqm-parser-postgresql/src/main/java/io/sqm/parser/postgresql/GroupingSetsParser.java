package io.sqm.parser.postgresql;

import io.sqm.core.GroupItem;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * PostgreSQL parser for {@code GROUPING SETS (...)} items.
 */
public class GroupingSetsParser implements MatchableParser<GroupItem.GroupingSets> {
    /**
     * Performs a look-ahead test to determine whether this parser is applicable
     * at the current cursor position.
     *
     * @param cur the cursor pointing at the current token
     * @param ctx the parsing context providing configuration and utilities
     * @return {@code true} if this parser should be used to parse the upcoming input
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.GROUPING);
    }

    /**
     * Parses {@code GROUPING SETS (...)}.
     *
     * @param cur a cursor containing tokens to parse
     * @param ctx a parser context containing parsers and lookups
     * @return a parsing result
     */
    @Override
    public ParseResult<? extends GroupItem.GroupingSets> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected GROUPING", TokenType.GROUPING);
        cur.expect("Expected SETS after GROUPING", TokenType.SETS);
        cur.expect("Expected ( after GROUPING SETS", TokenType.LPAREN);

        List<GroupItem> sets = new ArrayList<>();
        if (!cur.match(TokenType.RPAREN)) {
            ParseResult<List<GroupItem>> items = parseItems(GroupItem.class, cur, ctx);
            if (items.isError()) {
                return error(items);
            }
            sets = items.value();
        }

        cur.expect("Expected ) to close GROUPING SETS", TokenType.RPAREN);
        return ok((GroupItem.GroupingSets) GroupItem.groupingSets(sets));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler
     */
    @Override
    public Class<? extends GroupItem.GroupingSets> targetType() {
        return GroupItem.GroupingSets.class;
    }
}
