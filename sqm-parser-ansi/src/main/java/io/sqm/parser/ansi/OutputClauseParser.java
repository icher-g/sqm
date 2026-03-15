package io.sqm.parser.ansi;

import io.sqm.core.OutputClause;
import io.sqm.core.OutputInto;
import io.sqm.core.OutputItem;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses SQL Server-style {@code OUTPUT} clauses.
 */
public class OutputClauseParser implements Parser<OutputClause> {

    /**
     * Creates an output-clause parser.
     */
    public OutputClauseParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<OutputClause> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected OUTPUT", TokenType.OUTPUT);

        var items = parseItems(OutputItem.class, cur, ctx);
        if (items.isError()) {
            return error(items);
        }

        OutputInto into = null;
        if (cur.match(TokenType.INTO)) {
            var parsedInto = ctx.parse(OutputInto.class, cur);
            if (parsedInto.isError()) {
                return error(parsedInto);
            }
            into = parsedInto.value();
        }

        return ok(OutputClause.of(items.value(), into));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<OutputClause> targetType() {
        return OutputClause.class;
    }
}
