package io.sqm.parser.ansi;

import io.sqm.core.CteDef;
import io.sqm.core.Identifier;
import io.sqm.core.Query;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parser for common table expression definitions.
 */
public class CteDefParser implements Parser<CteDef> {
    /**
     * Creates a CTE definition parser.
     */
    public CteDefParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<CteDef> parse(Cursor cur, ParseContext ctx) {
        var name = toIdentifier(cur.expect("Expected CTE name", TokenType.IDENT));
        var aliases = new ArrayList<Identifier>();

        if (cur.consumeIf(TokenType.LPAREN)) {
            do {
                var alias = cur.expect("Expected column name", TokenType.IDENT);
                aliases.add(toIdentifier(alias));
            } while (cur.consumeIf(TokenType.COMMA));

            cur.expect("Expected ')'", TokenType.RPAREN);
        }

        cur.expect("Expected AS", TokenType.AS);

        CteDef.Materialization materialization = CteDef.Materialization.DEFAULT;
        if (cur.consumeIf(TokenType.MATERIALIZED)) {
            if (!ctx.capabilities().supports(SqlFeature.CTE_MATERIALIZATION)) {
                return error("CTE materialization is not supported by this dialect", cur.fullPos());
            }
            materialization = CteDef.Materialization.MATERIALIZED;
        }
        else if (cur.consumeIf(TokenType.NOT)) {
            if (!cur.consumeIf(TokenType.MATERIALIZED)) {
                return error("Expected MATERIALIZED after NOT", cur.fullPos());
            }
            if (!ctx.capabilities().supports(SqlFeature.CTE_MATERIALIZATION)) {
                return error("CTE materialization is not supported by this dialect", cur.fullPos());
            }
            materialization = CteDef.Materialization.NOT_MATERIALIZED;
        }
        cur.expect("Expected '(' before CTE subquery", TokenType.LPAREN);

        var body = ctx.parse(Query.class, cur);
        if (body.isError()) {
            return error(body);
        }

        cur.expect("Expected ')' after CTE subquery", TokenType.RPAREN);
        return ok(Query.cte(name, body.value(), aliases, materialization));
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
