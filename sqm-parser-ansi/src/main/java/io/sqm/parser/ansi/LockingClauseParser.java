package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.LockMode;
import io.sqm.core.LockTarget;
import io.sqm.core.LockWaitMode;
import io.sqm.core.LockingClause;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses row-locking clauses.
 */
public class LockingClauseParser implements Parser<LockingClause> {
    /**
     * Creates a locking-clause parser.
     */
    public LockingClauseParser() {
    }

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

        List<LockTarget> targets = List.of();
        if (cur.consumeIf(TokenType.OF)) {
            if (!ctx.capabilities().supports(SqlFeature.LOCKING_OF)) {
                return error("FOR UPDATE OF is not supported by this dialect", cur.fullPos());
            }
            targets = parseIdentifierItems(cur, "Expected table name after OF").stream()
                .map(LockTarget::of)
                .toList();
        }

        LockWaitMode waitMode = LockWaitMode.DEFAULT;
        Expression waitSeconds = null;

        if (cur.consumeIf(TokenType.NOWAIT)) {
            if (!ctx.capabilities().supports(SqlFeature.LOCKING_NOWAIT)) {
                return error("NOWAIT is not supported by this dialect", cur.fullPos());
            }
            waitMode = LockWaitMode.NOWAIT;
        }
        else if (cur.consumeIf(TokenType.SKIP)) {
            cur.expect("Expected LOCKED after SKIP", TokenType.LOCKED);
            if (!ctx.capabilities().supports(SqlFeature.LOCKING_SKIP_LOCKED)) {
                return error("SKIP LOCKED is not supported by this dialect", cur.fullPos());
            }
            waitMode = LockWaitMode.SKIP_LOCKED;
        }
        else if (cur.consumeIf(TokenType.WAIT)) {
            if (!ctx.capabilities().supports(SqlFeature.LOCKING_WAIT_TIMEOUT)) {
                return error("WAIT is not supported by this dialect", cur.fullPos());
            }
            var seconds = ctx.parse(Expression.class, cur);
            if (seconds.isError()) {
                return error(seconds);
            }
            waitMode = LockWaitMode.WAIT;
            waitSeconds = seconds.value();
        }

        return ok(LockingClause.of(mode, targets, waitMode, waitSeconds));
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
