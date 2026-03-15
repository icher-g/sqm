package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.OutputItem;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses one projected item inside an {@code OUTPUT} clause.
 */
public class OutputItemParser implements Parser<OutputItem> {

    /**
     * Creates an output-item parser.
     */
    public OutputItemParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<OutputItem> parse(Cursor cur, ParseContext ctx) {
        var expression = parseOutputExpression(cur, ctx);
        if (expression.isError()) {
            return error(expression);
        }

        return ok(OutputItem.of(expression.value(), parseAliasIdentifier(cur)));
    }

    /**
     * Parses the projected expression used by one output item.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return parsed output expression
     */
    protected ParseResult<? extends Expression> parseOutputExpression(Cursor cur, ParseContext ctx) {
        return ctx.parse(Expression.class, cur);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<OutputItem> targetType() {
        return OutputItem.class;
    }
}