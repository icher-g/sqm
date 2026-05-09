package io.sqm.parser.ansi;

import io.sqm.core.SequenceValueExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;

/**
 * Placeholder parser for sequence value expressions in dialects that do not support them.
 */
public class SequenceValueExprParser implements MatchableParser<SequenceValueExpr> {

    /**
     * Creates an ANSI sequence value expression parser.
     */
    public SequenceValueExprParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed
     * @param ctx a parser context containing parsers and lookups
     * @return an unsupported feature parsing result
     */
    @Override
    public ParseResult<? extends SequenceValueExpr> parse(Cursor cur, ParseContext ctx) {
        return error("Sequence value expressions are not supported by this dialect", cur.fullPos());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler
     */
    @Override
    public Class<SequenceValueExpr> targetType() {
        return SequenceValueExpr.class;
    }

    /**
     * Performs a look-ahead test to determine whether this parser is applicable.
     *
     * @param cur the cursor pointing at the current token
     * @param ctx the parsing context providing configuration and utilities
     * @return always {@code false} because ANSI does not own sequence syntax
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return false;
    }
}
