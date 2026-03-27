package io.sqm.parser.sqlserver;

import io.sqm.core.CrossJoin;
import io.sqm.core.Join;
import io.sqm.core.JoinKind;
import io.sqm.core.Lateral;
import io.sqm.core.OnJoin;
import io.sqm.core.Predicate;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * SQL Server join parser with support for {@code CROSS APPLY} and
 * {@code OUTER APPLY}.
 */
public final class JoinParser extends io.sqm.parser.JoinParser {
    /**
     * Creates a SQL Server join parser.
     */
    public JoinParser() {
    }

    /**
     * Parses SQL Server joins, including APPLY forms.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends Join> parse(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.CROSS) && cur.match(TokenType.APPLY, 1)) {
            cur.expect("Expected CROSS", TokenType.CROSS);
            cur.expect("Expected APPLY", TokenType.APPLY);
            var table = ctx.parse(io.sqm.core.TableRef.class, cur);
            if (table.isError()) {
                return error(table);
            }
            return ok(CrossJoin.of(Lateral.of(table.value())));
        }

        if (cur.match(TokenType.OUTER) && cur.match(TokenType.APPLY, 1)) {
            cur.expect("Expected OUTER", TokenType.OUTER);
            cur.expect("Expected APPLY", TokenType.APPLY);
            var table = ctx.parse(io.sqm.core.TableRef.class, cur);
            if (table.isError()) {
                return error(table);
            }
            return ok(OnJoin.of(Lateral.of(table.value()), JoinKind.LEFT, Predicate.unary(io.sqm.core.Expression.literal(true))));
        }

        return super.parse(cur, ctx);
    }
}