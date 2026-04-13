package io.sqm.parser.ansi;

import io.sqm.core.CteDef;
import io.sqm.core.Identifier;
import io.sqm.core.Query;
import io.sqm.core.Statement;
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
            aliases = new ArrayList<>(parseIdentifierItems(cur, "Expected column name"));
            cur.expect("Expected ')'", TokenType.RPAREN);
        }

        cur.expect("Expected AS", TokenType.AS);

        CteDef.Materialization materialization = CteDef.Materialization.DEFAULT;
        int modifierPos = cur.fullPos();
        if (cur.consumeIf(TokenType.MATERIALIZED)) {
            if (!ctx.capabilities().supports(SqlFeature.CTE_MATERIALIZATION)) {
                return error("CTE materialization is not supported by this dialect", modifierPos);
            }
            materialization = CteDef.Materialization.MATERIALIZED;
        }
        else if (cur.consumeIf(TokenType.NOT)) {
            cur.expect("Expected MATERIALIZED after NOT", TokenType.MATERIALIZED);
            if (!ctx.capabilities().supports(SqlFeature.CTE_MATERIALIZATION)) {
                return error("CTE materialization is not supported by this dialect", modifierPos);
            }
            materialization = CteDef.Materialization.NOT_MATERIALIZED;
        }
        cur.expect("Expected '(' before CTE subquery", TokenType.LPAREN);

        var body = parseBody(cur, ctx);
        if (body.isError()) {
            return error(body);
        }

        cur.expect("Expected ')' after CTE subquery", TokenType.RPAREN);
        return createCte(name, body.value(), aliases, materialization, ctx, cur);
    }

    /**
     * Parses CTE body statement.
     *
     * @param cur token cursor positioned at CTE body start.
     * @param ctx parse context.
     * @return parsed statement body.
     */
    protected ParseResult<? extends Statement> parseBody(Cursor cur, ParseContext ctx) {
        return ctx.parse(Query.class, cur);
    }

    /**
     * Creates CTE node from parsed parts.
     *
     * @param name CTE name.
     * @param body parsed body statement.
     * @param aliases column aliases.
     * @param materialization materialization hint.
     * @param ctx parse context.
     * @param cur token cursor.
     * @return parsed CTE definition.
     */
    protected ParseResult<CteDef> createCte(Identifier name,
                                            Statement body,
                                            List<Identifier> aliases,
                                            CteDef.Materialization materialization,
                                            ParseContext ctx,
                                            Cursor cur) {
        if (body instanceof Query query) {
            return ok(Query.cte(name, query, aliases, materialization));
        }
        return error("Writable CTE DML is not supported by this dialect", cur.fullPos());
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
