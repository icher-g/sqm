package io.sqm.parser.ansi;

import io.sqm.core.LockMode;
import io.sqm.core.LockingClause;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.List;

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
        cur.expect("Expected UPDATE", TokenType.UPDATE);

        if (cur.match(TokenType.OF)) {
            throw new UnsupportedDialectFeatureException("OF t1, t2", "ANSI");
        }

        if (cur.match(TokenType.NOWAIT)) {
            throw new UnsupportedDialectFeatureException("NOWAIT", "ANSI");
        }

        if (cur.match(TokenType.SKIP)) {
            throw new UnsupportedDialectFeatureException("SKIP LOCKED", "ANSI");
        }

        return ok(LockingClause.of(LockMode.UPDATE, List.of(), false, false));
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
