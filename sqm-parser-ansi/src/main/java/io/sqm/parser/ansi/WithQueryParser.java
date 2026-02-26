package io.sqm.parser.ansi;

import io.sqm.core.CompositeQuery;
import io.sqm.core.CteDef;
import io.sqm.core.Query;
import io.sqm.core.WithQuery;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses queries with {@code WITH} clauses.
 */
public class WithQueryParser implements Parser<WithQuery> {
    /**
     * Creates a with-query parser.
     */
    public WithQueryParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<WithQuery> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected WITH.", TokenType.WITH);

        final boolean recursive = cur.consumeIf(TokenType.RECURSIVE);

        List<CteDef> ctes = new ArrayList<>();
        do {
            var cte = ctx.parse(CteDef.class, cur);
            if (cte.isError()) {
                return error(cte);
            }
            ctes.add(cte.value());
        } while (cur.consumeIf(TokenType.COMMA));

        var body = ctx.parse(CompositeQuery.class, cur);
        if (body.isError()) {
            return error(body);
        }
        return ok(Query.with(ctes, body.value(), recursive));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<WithQuery> targetType() {
        return WithQuery.class;
    }
}
