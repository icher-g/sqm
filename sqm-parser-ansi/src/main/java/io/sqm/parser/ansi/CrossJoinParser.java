package io.sqm.parser.ansi;

import io.sqm.core.CrossJoin;
import io.sqm.core.Join;
import io.sqm.core.TableRef;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parser for CROSS JOIN clauses.
 */
public class CrossJoinParser implements MatchableParser<CrossJoin> {
    /**
     * Creates a cross-join parser.
     */
    public CrossJoinParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<CrossJoin> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected CROSS", TokenType.CROSS);
        cur.expect("Expected JOIN", TokenType.JOIN);

        var table = ctx.parse(TableRef.class, cur);
        if (table.isError()) {
            return error(table);
        }
        return ok(Join.cross(table.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<CrossJoin> targetType() {
        return CrossJoin.class;
    }

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
        return cur.match(TokenType.CROSS);
    }
}
