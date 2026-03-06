package io.sqm.parser.mysql;

import io.sqm.core.GroupBy;
import io.sqm.core.GroupItem;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses MySQL {@code GROUP BY} including the keyword form
 * {@code GROUP BY ... WITH ROLLUP}.
 */
public class MySqlGroupByParser implements Parser<GroupBy> {

    /**
     * Creates a MySQL GROUP BY parser.
     */
    public MySqlGroupByParser() {
    }

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

        var items = parseItems(GroupItem.class, cur, ctx);
        if (items.isError()) {
            return error(items);
        }

        if (cur.consumeIf(TokenType.WITH)) {
            cur.expect("Expected ROLLUP after WITH", TokenType.ROLLUP);
            if (!ctx.capabilities().supports(SqlFeature.ROLLUP)) {
                return error("ROLLUP is not supported by this dialect", cur.fullPos());
            }
            return ok(GroupBy.of(List.of(GroupItem.rollup(items.value()))));
        }

        return ok(GroupBy.of(items.value()));
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
