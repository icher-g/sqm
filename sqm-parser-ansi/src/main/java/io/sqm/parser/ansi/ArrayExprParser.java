package io.sqm.parser.ansi;

import io.sqm.core.ArrayExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class ArrayExprParser implements Parser<ArrayExpr> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends ArrayExpr> parse(Cursor cur, ParseContext ctx) {
        throw new UnsupportedOperationException(
            "Array expressions are not supported by ANSI SQL parser"
        );
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends ArrayExpr> targetType() {
        return ArrayExpr.class;
    }
}
