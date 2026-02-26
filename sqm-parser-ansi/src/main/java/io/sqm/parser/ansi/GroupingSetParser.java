package io.sqm.parser.ansi;

import io.sqm.core.GroupItem;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * ANSI parser stub for grouping set items.
 */
public class GroupingSetParser implements MatchableParser<GroupItem.GroupingSet> {
    /**
     * Creates a grouping-set parser.
     */
    public GroupingSetParser() {
    }

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
        return cur.match(TokenType.LPAREN);
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends GroupItem.GroupingSet> parse(Cursor cur, ParseContext ctx) {
        if (!ctx.capabilities().supports(SqlFeature.GROUPING_SETS)) {
            return error("GROUPING SETS are not supported by this dialect", cur.fullPos());
        }
        cur.expect("Expected (", TokenType.LPAREN);
        if (cur.consumeIf(TokenType.RPAREN)) {
            return ok((GroupItem.GroupingSet) GroupItem.groupingSet(List.of()));
        }

        ParseResult<List<GroupItem>> items = parseItems(GroupItem.SimpleGroupItem.class, cur, ctx);
        if (items.isError()) {
            return error(items);
        }

        cur.expect("Expected ) to close grouping set", TokenType.RPAREN);
        return ok((GroupItem.GroupingSet) GroupItem.groupingSet(items.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends GroupItem.GroupingSet> targetType() {
        return GroupItem.GroupingSet.class;
    }
}
