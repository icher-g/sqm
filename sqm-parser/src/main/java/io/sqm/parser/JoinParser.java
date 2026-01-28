package io.sqm.parser;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchResult;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.Map;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

public class JoinParser implements Parser<Join> {

    private static final Map<TokenType, JoinKind> token2join = Map.of(
        TokenType.INNER, JoinKind.INNER,
        TokenType.LEFT, JoinKind.LEFT,
        TokenType.RIGHT, JoinKind.RIGHT,
        TokenType.FULL, JoinKind.FULL
    );

    /**
     * Parses {@link JoinKind} from the {@link Cursor}.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @return a parsed join kind.
     */
    public static JoinKind parseKind(Cursor cur) {
        JoinKind kind = JoinKind.INNER;
        if (cur.matchAny(TokenType.INNER, TokenType.LEFT, TokenType.RIGHT, TokenType.FULL, TokenType.CROSS)) {
            kind = token2join.get(cur.advance().type());
            cur.consumeIf(TokenType.OUTER); // LEFT/RIGHT/FULL OUTER
        }
        return kind;
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends Join> parse(Cursor cur, ParseContext ctx) {
        MatchResult<? extends Join> matched = ctx.parseIfMatch(CrossJoin.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        matched = ctx.parseIfMatch(NaturalJoin.class, cur);
        if (matched.match()) {
            return matched.result();
        }

        JoinKind kind = parseKind(cur);

        // JOIN keyword
        cur.expect("Expected JOIN", TokenType.JOIN);

        var table = ctx.parse(TableRef.class, cur);
        if (table.isError()) {
            return error(table);
        }

        matched = ctx.parseIfMatch(UsingJoin.class, table.value(), cur);
        if (matched.match()) {
            if (matched.result().isError()) {
                return matched.result();
            }
            var usingJoin = (UsingJoin) matched.result().value();
            return ok(usingJoin.ofKind(kind));
        }

        matched = ctx.parseIfMatch(OnJoin.class, table.value(), cur);
        if (matched.match()) {
            if (matched.result().isError()) {
                return matched.result();
            }
            var onJoin = (OnJoin) matched.result().value();
            return ok(onJoin.ofKind(kind));
        }

        return error("The specified join type: " + cur.peek().lexeme() + " is not supported.", cur.fullPos());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<Join> targetType() {
        return Join.class;
    }
}
