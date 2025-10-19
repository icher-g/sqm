package io.sqm.parser.ansi.column;

import io.sqm.core.StarColumn;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * A parser to parse a '*' as a column.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     SELECT * FROM t;
 *     }
 * </pre>
 */
public class StarColumnParser implements Parser<StarColumn> {

    /**
     * Parses '*' as a column.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<StarColumn> parse(Cursor cur, ParseContext ctx) {
        if (cur.consumeIf(TokenType.STAR)) {
            return ok(new StarColumn());
        }
        return error("Expected *", cur.fullPos());
    }

    /**
     * Gets {@link StarColumn} as a target type.
     *
     * @return {@link StarColumn}.
     */
    @Override
    public Class<StarColumn> targetType() {
        return StarColumn.class;
    }
}
