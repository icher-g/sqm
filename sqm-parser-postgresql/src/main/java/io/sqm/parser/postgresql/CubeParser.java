package io.sqm.parser.postgresql;

import io.sqm.core.GroupItem;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * PostgreSQL parser for {@code CUBE (...)} items.
 */
public class CubeParser implements MatchableParser<GroupItem.Cube> {
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
        return cur.match(TokenType.CUBE);
    }

    /**
     * Parses {@code CUBE (...)}.
     *
     * @param cur a cursor containing tokens to parse
     * @param ctx a parser context containing parsers and lookups
     * @return a parsing result
     */
    @Override
    public ParseResult<? extends GroupItem.Cube> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected CUBE", TokenType.CUBE);
        cur.expect("Expected ( after CUBE", TokenType.LPAREN);

        if (cur.match(TokenType.RPAREN)) {
            return error("CUBE requires at least one grouping item", cur.fullPos());
        }

        ParseResult<List<GroupItem>> items = parseItems(GroupItem.SimpleGroupItem.class, cur, ctx);
        if (items.isError()) {
            return error(items);
        }

        cur.expect("Expected ) to close CUBE", TokenType.RPAREN);
        return ok((GroupItem.Cube) GroupItem.cube(items.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler
     */
    @Override
    public Class<? extends GroupItem.Cube> targetType() {
        return GroupItem.Cube.class;
    }
}
