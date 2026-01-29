package io.sqm.parser.postgresql;

import io.sqm.core.LockMode;
import io.sqm.core.LockTarget;
import io.sqm.core.LockingClause;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

public class LockingClauseParser implements Parser<LockingClause> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends LockingClause> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected FOR", TokenType.FOR);
        var t = cur.advance();

        LockMode mode;

        switch (t.lexeme().toLowerCase()) {
            case "update": {
                mode = LockMode.UPDATE;
            }
            break;
            case "share": {
                mode = LockMode.SHARE;
            }
            break;
            case "key": {
                cur.expect("Expected SHARE", TokenType.SHARE);
                mode = LockMode.KEY_SHARE;
            }
            break;
            case "no": {
                cur.expect("Expected KEY", TokenType.KEY);
                cur.expect("Expected UPDATE", TokenType.UPDATE);
                mode = LockMode.NO_KEY_UPDATE;
            }
            break;
            default: {
                return error("Unexpected token: " + t.lexeme(), cur.fullPos());
            }
        }

        List<LockTarget> targets = new ArrayList<>();
        if (cur.consumeIf(TokenType.OF)) {
            do {
                var item = cur.advance();
                targets.add(LockTarget.of(item.lexeme()));
            }
            while (cur.consumeIf(TokenType.COMMA));
        }

        var nowait = cur.consumeIf(TokenType.NOWAIT);
        var skipLocked = cur.consumeIf(TokenType.SKIP) && cur.consumeIf(TokenType.LOCKED);

        return ok(LockingClause.of(mode, targets, nowait, skipLocked));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends LockingClause> targetType() {
        return LockingClause.class;
    }
}
