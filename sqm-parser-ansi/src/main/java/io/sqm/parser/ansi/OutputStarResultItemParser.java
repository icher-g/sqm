package io.sqm.parser.ansi;

import io.sqm.core.OutputStarResultItem;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;

/**
 * Rejects SQL Server pseudo-row-source star items in ANSI parsing.
 */
public class OutputStarResultItemParser implements MatchableParser<OutputStarResultItem> {

    /**
     * Creates an output-star result-item parser.
     */
    public OutputStarResultItemParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<OutputStarResultItem> parse(Cursor cur, ParseContext ctx) {
        return error("OUTPUT pseudo-row-source stars are not supported by this dialect", cur.fullPos());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<OutputStarResultItem> targetType() {
        return OutputStarResultItem.class;
    }

    /**
     * Performs a look-ahead test to determine whether this parser is applicable
     * at the current cursor position.
     *
     * @param cur the current cursor pointing to the next token to be parsed
     * @param ctx the parsing context providing configuration, helpers and nested parsing
     * @return {@code false} because ANSI never parses SQL Server output stars
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return false;
    }
}
