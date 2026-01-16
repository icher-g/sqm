package io.sqm.parser.ansi;

import io.sqm.core.QualifiedStarSelectItem;
import io.sqm.core.SelectItem;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.core.OperatorTokens.isStar;
import static io.sqm.parser.spi.ParseResult.ok;

public class QualifiedStarSelectItemParser implements MatchableParser<QualifiedStarSelectItem> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<QualifiedStarSelectItem> parse(Cursor cur, ParseContext ctx) {
        var t = cur.expect("Expected identifier", TokenType.IDENT);
        cur.expect("Expected '.'", TokenType.DOT);
        cur.expect("Expected '*'", token -> isStar(token));
        return ok(SelectItem.star(t.lexeme()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<QualifiedStarSelectItem> targetType() {
        return QualifiedStarSelectItem.class;
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
        // c.*
        return cur.match(TokenType.IDENT) && cur.match(TokenType.DOT, 1) && isStar(cur.peek(2));
    }
}
