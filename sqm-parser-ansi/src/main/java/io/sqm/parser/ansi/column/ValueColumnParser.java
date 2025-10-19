package io.sqm.parser.ansi.column;

import io.sqm.core.ValueColumn;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * A parser to parse a value as a column.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     SELECT 1;
 *     }
 * </pre>
 */
public class ValueColumnParser implements Parser<ValueColumn> {

    /**
     * Parses value as a column.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<ValueColumn> parse(Cursor cur, ParseContext ctx) {
        var t = cur.advance();
        Object value = t.lexeme();
        if (t.type() == TokenType.NUMBER) {
            value = parseNumber(t.lexeme());
        }
        var alias = parseAlias(cur);
        return ok(new ValueColumn(value, alias));
    }

    /**
     * Gets a {@link ValueColumn} as a target type.
     *
     * @return {@link ValueColumn}.
     */
    @Override
    public Class<ValueColumn> targetType() {
        return ValueColumn.class;
    }
}
