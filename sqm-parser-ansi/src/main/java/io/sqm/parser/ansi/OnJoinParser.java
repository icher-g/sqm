package io.sqm.parser.ansi;

import io.sqm.core.JoinKind;
import io.sqm.core.OnJoin;
import io.sqm.core.Predicate;
import io.sqm.core.TableRef;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

public class OnJoinParser implements MatchableParser<OnJoin> {

    private static JoinKind mapJoinType(TokenType tt) {
        return switch (tt) {
            case LEFT -> JoinKind.LEFT;
            case RIGHT -> JoinKind.RIGHT;
            case FULL -> JoinKind.FULL;
            default -> JoinKind.INNER;
        };
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<OnJoin> parse(Cursor cur, ParseContext ctx) {
        // Optional join kind
        JoinKind kind = JoinKind.INNER;

        if (cur.matchAny(TokenType.INNER, TokenType.LEFT, TokenType.RIGHT, TokenType.FULL, TokenType.CROSS)) {
            kind = mapJoinType(cur.advance().type());
            cur.consumeIf(TokenType.OUTER); // LEFT/RIGHT/FULL OUTER
        }

        // JOIN keyword
        cur.expect("Expected JOIN", TokenType.JOIN);

        // Table
        var table = ctx.parse(TableRef.class, cur);
        if (table.isError()) {
            return error(table);
        }

        // ON <expr>
        cur.expect("Expected ON", TokenType.ON);

        var on = ctx.parse(Predicate.class, cur);
        if (on.isError()) {
            return error(on);
        }

        // 6) Build the Join
        return ok(OnJoin.of(table.value(), kind, on.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<OnJoin> targetType() {
        return OnJoin.class;
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
        return cur.matchAny(TokenType.INNER, TokenType.LEFT, TokenType.RIGHT, TokenType.FULL, TokenType.CROSS, TokenType.JOIN);
    }
}
