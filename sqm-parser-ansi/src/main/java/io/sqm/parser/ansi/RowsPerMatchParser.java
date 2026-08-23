package io.sqm.parser.ansi;

import io.sqm.core.RowsPerMatch;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses a row-count mode in {@code MATCH_RECOGNIZE}.
 */
public class RowsPerMatchParser implements Parser<RowsPerMatch> {
    /**
     * Creates a rows-per-match parser.
     */
    public RowsPerMatchParser() {
    }

    private static void expectMatches(Cursor cur, String message) {
        var token = cur.expect(message, TokenType.IDENT);
        if (!"MATCHES".equalsIgnoreCase(token.lexeme())) {
            throw new io.sqm.parser.core.ParserException(message, token.pos());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends RowsPerMatch> parse(Cursor cur, ParseContext ctx) {
        if (cur.consumeIf(TokenType.ONE)) {
            cur.expect("Expected ROW after ONE", TokenType.ROW);
            cur.expect("Expected PER after ONE ROW", TokenType.PER);
            cur.expect("Expected MATCH after ONE ROW PER", TokenType.MATCH);
            return ok(RowsPerMatch.of(RowsPerMatch.Mode.ONE, RowsPerMatch.EmptyMatchHandling.DEFAULT));
        }
        cur.expect("Expected ALL or ONE", TokenType.ALL);
        cur.expect("Expected ROWS after ALL", TokenType.ROWS);
        cur.expect("Expected PER after ALL ROWS", TokenType.PER);
        cur.expect("Expected MATCH after ALL ROWS PER", TokenType.MATCH);
        var handling = RowsPerMatch.EmptyMatchHandling.DEFAULT;
        if (cur.consumeIf(TokenType.SHOW)) {
            cur.expect("Expected EMPTY after SHOW", TokenType.EMPTY);
            expectMatches(cur, "Expected MATCHES after SHOW EMPTY");
            handling = RowsPerMatch.EmptyMatchHandling.SHOW_EMPTY;
        }
        else {
            if (cur.consumeIf(TokenType.OMIT)) {
                cur.expect("Expected EMPTY after OMIT", TokenType.EMPTY);
                expectMatches(cur, "Expected MATCHES after OMIT EMPTY");
                handling = RowsPerMatch.EmptyMatchHandling.OMIT_EMPTY;
            }
            else {
                if (cur.consumeIf(TokenType.WITH)) {
                    cur.expect("Expected UNMATCHED after WITH", TokenType.UNMATCHED);
                    cur.expect("Expected ROWS after WITH UNMATCHED", TokenType.ROWS);
                    handling = RowsPerMatch.EmptyMatchHandling.WITH_UNMATCHED;
                }
            }
        }
        return ok(RowsPerMatch.of(RowsPerMatch.Mode.ALL, handling));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<RowsPerMatch> targetType() {
        return RowsPerMatch.class;
    }
}
