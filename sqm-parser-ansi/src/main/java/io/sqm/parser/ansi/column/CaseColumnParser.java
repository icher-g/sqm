package io.sqm.parser.ansi.column;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;

/**
 * A parser used to parse a CASE statement.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     CASE
 *        WHEN condition1 THEN result1
 *        WHEN condition2 THEN result2
 *        WHEN conditionN THEN resultN
 *        ELSE result
 *     END;
 *     }
 * </pre>
 */
public class CaseColumnParser implements Parser<CaseColumn> {

    /**
     * Parses SQL CASE statement.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<CaseColumn> parse(Cursor cur, ParseContext ctx) {
        // CASE
        cur.expect("Expected CASE but found: " + cur.peek(), TokenType.CASE);

        // One or more WHEN ... THEN ...
        var whens = new ArrayList<WhenThen>();
        while (cur.consumeIf(TokenType.WHEN)) {
            var end = cur.find(TokenType.THEN);
            if (end == cur.size()) {
                return error("Expected THEN after WHEN <predicate>", cur.fullPos());
            }

            var fr = ctx.parse(Filter.class, cur.advance(end));
            if (fr.isError()) {
                return error(fr);
            }

            // THEN
            cur.expect("Expected THEN after WHEN <predicate>", TokenType.THEN);

            // <result>
            var thenResult = parseResultExpr(cur, ctx);
            if (thenResult.isError()) {
                return error(thenResult);
            }

            whens.add(new WhenThen(fr.value(), thenResult.value()));
        }

        if (whens.isEmpty()) {
            return error("CASE must have at least one WHEN ... THEN arm", cur.fullPos());
        }

        // Optional ELSE <result>
        Entity elseValue = null;

        if (cur.consumeIf(TokenType.ELSE)) {
            var elseResult = parseResultExpr(cur, ctx);
            if (elseResult.isError()) {
                return error(elseResult);
            }
            elseValue = elseResult.value();
        }

        // END
        cur.expect("Expected END to close CASE", TokenType.END);

        // Optional AS alias
        var alias = parseAlias(cur);
        return ok(new CaseColumn(whens, elseValue, alias));
    }

    @Override
    public Class<CaseColumn> targetType() {
        return CaseColumn.class;
    }

    /**
     * Parses a CASE result expr:
     * - string/number/boolean/null literal → boxed Java Object
     * - qualified identifier like  t.col  or  "T"."Name"  → Column.of(...).from(...)
     * - parenthesized CASE (nested) → delegate to this parser (optional; shown here)
     * <p>
     * If you already have a ColumnSpecParser / ValueSpecParser, swap them in here.
     */
    private ParseResult<Entity> parseResultExpr(Cursor cur, ParseContext ctx) {
        // Literals
        if (cur.match(TokenType.STRING)) {
            return ok(Values.single(cur.advance().lexeme()));
        }
        if (cur.match(TokenType.NUMBER)) {
            return ok(Values.single(parseNumber(cur.advance().lexeme())));
        }
        if (cur.match(TokenType.NULL)) {
            cur.advance(); // skip the literal itself
            return ok(Values.single(null));
        }
        if (cur.match(TokenType.TRUE)) {
            cur.advance(); // skip the literal itself
            return ok(Values.single(Boolean.TRUE));
        }
        if (cur.match(TokenType.FALSE)) {
            cur.advance(); // skip the literal itself
            return ok(Values.single(Boolean.FALSE));
        }

        // Nested CASE
        if (cur.match(TokenType.CASE)) {
            var pr = parse(cur, ctx);
            if (pr.isError()) {
                return error(pr);
            }
            return ok(pr.value());
        }

        // Otherwise: treat as a column reference (possibly qualified)
        var columnResult = ctx.parse(NamedColumn.class, cur);
        if (columnResult.ok()) {
            return ok(columnResult.value());
        }
        return error("Expected literal, column, or nested CASE", cur.fullPos());
    }
}
