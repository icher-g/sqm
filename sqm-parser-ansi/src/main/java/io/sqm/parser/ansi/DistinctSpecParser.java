package io.sqm.parser.ansi;

import io.sqm.core.DistinctSpec;
import io.sqm.core.Expression;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

public class DistinctSpecParser implements Parser<DistinctSpec> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends DistinctSpec> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected DISTINCT", TokenType.DISTINCT);
        if (cur.consumeIf(TokenType.ON)) {
            if (!ctx.capabilities().supports(SqlFeature.DISTINCT_ON)) {
                return error("DISTINCT ON is not supported by this dialect", cur.fullPos());
            }
            cur.expect("Expected ( after DISTINCT ON", TokenType.LPAREN);
            var items = parseItems(Expression.class, cur, ctx);
            if (items.isError()) {
                return error(items);
            }
            cur.expect("Expected ) to close expressions list", TokenType.RPAREN);
            return ok(DistinctSpec.on(items.value()));
        }
        return ok(DistinctSpec.TRUE);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends DistinctSpec> targetType() {
        return DistinctSpec.class;
    }
}
