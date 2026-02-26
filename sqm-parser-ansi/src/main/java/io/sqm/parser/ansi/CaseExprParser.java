package io.sqm.parser.ansi;

import io.sqm.core.CaseExpr;
import io.sqm.core.Expression;
import io.sqm.core.WhenThen;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.ArrayList;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses {@code CASE ... WHEN ... THEN ... ELSE ... END} expressions.
 */
public class CaseExprParser implements MatchableParser<CaseExpr> {
    /**
     * Creates a case expression parser.
     */
    public CaseExprParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<CaseExpr> parse(Cursor cur, ParseContext ctx) {
        // CASE
        cur.expect("Expected CASE but found: " + cur.peek(), TokenType.CASE);

        // One or more WHEN ... THEN ...
        var whens = new ArrayList<WhenThen>();
        while (cur.match(TokenType.WHEN)) {
            var whenThen = ctx.parse(WhenThen.class, cur);
            if (whenThen.isError()) {
                return error(whenThen);
            }
            whens.add(whenThen.value());
        }

        if (whens.isEmpty()) {
            return error("CASE must have at least one WHEN ... THEN arm", cur.fullPos());
        }

        // Optional ELSE <result>
        Expression elseValue = null;

        if (cur.consumeIf(TokenType.ELSE)) {
            var elseResult = ctx.parse(Expression.class, cur);
            if (elseResult.isError()) {
                return error(elseResult);
            }
            elseValue = elseResult.value();
        }

        // END
        cur.expect("Expected END to close CASE", TokenType.END);

        return ok(CaseExpr.of(whens, elseValue));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<CaseExpr> targetType() {
        return CaseExpr.class;
    }

    /**
     * Performs a look-ahead test to determine whether this parser is applicable
     * at the current cursor position.
     * <p>
     * The method must <strong>not</strong> advance the cursor or modify any parsing
     * context state. Its sole responsibility is to check whether the upcoming
     * tokens syntactically correspond to the construct handled by this parser.
     *
     * @param cur the current cursor pointing to the next token to be parsed
     * @param ctx the parsing context providing configuration, helpers and nested parsing
     * @return {@code true} if this parser should be used to parse the upcoming
     * construct, {@code false} otherwise
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.CASE);
    }
}
