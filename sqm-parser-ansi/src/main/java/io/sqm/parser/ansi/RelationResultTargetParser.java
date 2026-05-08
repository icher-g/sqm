package io.sqm.parser.ansi;

import io.sqm.core.RelationResultTarget;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;

/**
 * Parses {@code OUTPUT / RETURNING ... INTO ...} targets.
 */
public class RelationResultTargetParser implements Parser<RelationResultTarget> {

    /**
     * Creates a result-into parser.
     */
    public RelationResultTargetParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<RelationResultTarget> parse(Cursor cur, ParseContext ctx) {
        return error("OUTPUT / RETURNING ... INTO ... is not supported by this dialect", cur.fullPos());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<RelationResultTarget> targetType() {
        return RelationResultTarget.class;
    }
}