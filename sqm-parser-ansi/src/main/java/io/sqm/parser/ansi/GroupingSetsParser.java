package io.sqm.parser.ansi;

import io.sqm.core.GroupItem;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;

/**
 * ANSI parser stub for {@code GROUPING SETS (...)}.
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
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends GroupItem.GroupingSets> parse(Cursor cur, ParseContext ctx) {
        return error("GROUPING SETS is not supported by ANSI SQL parser", cur.fullPos());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends GroupItem.GroupingSets> targetType() {
        return GroupItem.GroupingSets.class;
    }
}
