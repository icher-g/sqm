package io.sqm.parser.ansi;

import io.sqm.core.CteDef;
import io.sqm.core.Query;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;

public class CteDefParser implements Parser<CteDef> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<CteDef> parse(Cursor cur, ParseContext ctx) {
        var name = cur.expect("Expected CTE name", TokenType.IDENT);
        var aliases = new ArrayList<String>();

        if (cur.consumeIf(TokenType.LPAREN)) {
            do {
                var alias = cur.expect("Expected column name", TokenType.IDENT);
                aliases.add(alias.lexeme());
            } while (cur.consumeIf(TokenType.COMMA));

            cur.expect("Expected ')'", TokenType.RPAREN);
        }

        cur.expect("Expected AS", TokenType.AS);
        cur.expect("Expected '(' before CTE subquery", TokenType.LPAREN);

        var body = ctx.parse(Query.class, cur);
        if (body.isError()) {
            return error(body);
        }

        cur.expect("Expected ')' after CTE subquery", TokenType.RPAREN);
        return finalize(cur, ctx, Query.cte(name.lexeme(), body.value(), aliases));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<CteDef> targetType() {
        return CteDef.class;
    }
}
