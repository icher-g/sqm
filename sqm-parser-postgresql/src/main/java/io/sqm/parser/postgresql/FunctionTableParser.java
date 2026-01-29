package io.sqm.parser.postgresql;

import io.sqm.core.FunctionExpr;
import io.sqm.core.FunctionTable;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

public class FunctionTableParser implements MatchableParser<FunctionTable> {
    /**
     * Performs a look-ahead test to determine whether this parser is applicable
     * at the current cursor position.
     * <p>
     * Implementations must <strong>not</strong> advance the cursor or modify
     * the {@link ParseContext}. Their sole responsibility is to inspect the
     * upcoming tokens and decide if this parser is responsible for them.
     *
     * @param cur the cursor pointing at the current token
     * @param ctx the parsing context providing configuration and utilities
     * @return {@code true} if this parser should be used to parse the upcoming
     * input, {@code false} otherwise
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return ctx.lookups().looksLikeFunctionCall(cur);
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends FunctionTable> parse(Cursor cur, ParseContext ctx) {
        var funcExpr = ctx.parse(FunctionExpr.class, cur);
        if (funcExpr.isError()) {
            return error(funcExpr);
        }

        var aliases = parseColumnAliases(cur);
        return ok(FunctionTable.of(funcExpr.value(), aliases.second(), aliases.first()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends FunctionTable> targetType() {
        return FunctionTable.class;
    }
}
