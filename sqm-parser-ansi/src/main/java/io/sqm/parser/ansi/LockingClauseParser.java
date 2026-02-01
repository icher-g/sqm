package io.sqm.parser.ansi;

import io.sqm.core.LockMode;
import io.sqm.core.LockTarget;
import io.sqm.core.LockingClause;
import io.sqm.core.dialect.SqlFeature;
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
                if (!ctx.capabilities().supports(SqlFeature.LOCKING_CLAUSE)) {
                    return error("FOR UPDATE is not supported by this dialect", cur.fullPos());
                }
                mode = LockMode.UPDATE;
            }
            break;
            case "share": {
                if (!ctx.capabilities().supports(SqlFeature.LOCKING_SHARE)) {
                    return error("FOR SHARE is not supported by this dialect", cur.fullPos());
                }
                mode = LockMode.SHARE;
            }
            break;
            case "key": {
                if (!ctx.capabilities().supports(SqlFeature.LOCKING_KEY_SHARE)) {
                    return error("FOR KEY SHARE is not supported by this dialect", cur.fullPos());
                }
                cur.expect("Expected SHARE", TokenType.SHARE);
                mode = LockMode.KEY_SHARE;
            }
            break;
            case "no": {
                if (!ctx.capabilities().supports(SqlFeature.LOCKING_NO_KEY_UPDATE)) {
                    return error("FOR NO KEY UPDATE is not supported by this dialect", cur.fullPos());
                }
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
            if (!ctx.capabilities().supports(SqlFeature.LOCKING_OF)) {
                return error("FOR UPDATE OF is not supported by this dialect", cur.fullPos());
            }
            do {
                var item = cur.expect("Expected table name after OF", TokenType.IDENT);
                targets.add(LockTarget.of(item.lexeme()));
            }
            while (cur.consumeIf(TokenType.COMMA));
        }

        boolean nowait = false;
        if (cur.consumeIf(TokenType.NOWAIT)) {
            if (!ctx.capabilities().supports(SqlFeature.LOCKING_NOWAIT)) {
                return error("NOWAIT is not supported by this dialect", cur.fullPos());
            }
            nowait = true;
        }

        boolean skipLocked = false;
        if (cur.consumeIf(TokenType.SKIP)) {
            if (!cur.consumeIf(TokenType.LOCKED)) {
                return error("Expected LOCKED after SKIP", cur.fullPos());
            }
            if (!ctx.capabilities().supports(SqlFeature.LOCKING_SKIP_LOCKED)) {
                return error("SKIP LOCKED is not supported by this dialect", cur.fullPos());
            }
            skipLocked = true;
        }

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
