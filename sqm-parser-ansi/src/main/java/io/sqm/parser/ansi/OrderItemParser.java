package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.utils.Numbers;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.Set;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses ORDER BY item specifications.
 */
public class OrderItemParser implements Parser<OrderItem> {

    /**
     * Creates an order-item parser.
     */
    public OrderItemParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<OrderItem> parse(Cursor cur, ParseContext ctx) {
        Integer ordinal = null;
        Expression expr = null;
        QualifiedName collateName = null;

        // Positional ORDER BY: "1", "2", ...
        if (Numbers.isPositiveInteger(cur.peek().lexeme())) {
            int pos = Integer.parseInt(cur.peek().lexeme());
            if (pos <= 0) {
                return error("ORDER BY position must be a positive integer", pos);
            }
            ordinal = pos;
            cur.advance();
        }
        else {
            // Otherwise: delegate to the column parser
            var result = ctx.parse(Expression.class, cur);
            if (result.isError()) {
                return error(result);
            }
            expr = result.value();
            if (expr instanceof CollateExpr collateExpr) {
                collateName = collateExpr.collation();
                expr = collateExpr.expr();
            }
        }

        // 3) Parse optional modifiers in any order from the remaining tokens
        Direction direction = null;
        Nulls nulls = null;
        String usingOperator = null;

        var tokens = Set.of(TokenType.ASC, TokenType.DESC, TokenType.NULLS, TokenType.COLLATE, TokenType.USING);

        while (cur.matchAny(tokens)) {
            if (cur.consumeIf(TokenType.ASC)) {
                if (direction != null) {
                    return error("Direction specified more than once", cur.fullPos());
                }
                if (usingOperator != null) {
                    return error("USING operator cannot be combined with ASC/DESC", cur.fullPos());
                }
                direction = Direction.ASC;
                continue;
            }
            if (cur.consumeIf(TokenType.DESC)) {
                if (direction != null) {
                    return error("Direction specified more than once", cur.fullPos());
                }
                if (usingOperator != null) {
                    return error("USING operator cannot be combined with ASC/DESC", cur.fullPos());
                }
                direction = Direction.DESC;
                continue;
            }
            if (cur.consumeIf(TokenType.NULLS)) {
                if (nulls != null) return error("NULLS specified more than once", cur.fullPos());
                var t = cur.expect("Expected FIRST | LAST | DEFAULT after NULLS", TokenType.FIRST, TokenType.LAST, TokenType.DEFAULT);
                if (t.type() == TokenType.FIRST) nulls = Nulls.FIRST;
                else
                    if (t.type() == TokenType.LAST) nulls = Nulls.LAST;
                    else
                        if (t.type() == TokenType.DEFAULT) nulls = Nulls.DEFAULT;
                        else return error("Expected FIRST | LAST | DEFAULT after NULLS", cur.fullPos());
                continue;
            }
            if (cur.consumeIf(TokenType.COLLATE)) {
                if (collateName != null) {
                    return error("COLLATE specified more than once", cur.fullPos());
                }
                var t = cur.expect("Expected collation name after COLLATE", TokenType.IDENT);
                collateName = parseQualifiedName(toIdentifier(t), cur);
                continue;
            }
            if (cur.consumeIf(TokenType.USING)) {
                if (!ctx.capabilities().supports(SqlFeature.ORDER_BY_USING)) {
                    return error("ORDER BY ... USING is not supported by this dialect", cur.fullPos());
                }
                if (usingOperator != null) {
                    return error("USING specified more than once", cur.fullPos());
                }
                if (direction != null) {
                    return error("USING operator cannot be combined with ASC/DESC", cur.fullPos());
                }
                if (cur.match(TokenType.OPERATOR)) {
                    usingOperator = cur.advance().lexeme();
                }
                else
                    if (cur.match(TokenType.QMARK)) {
                        usingOperator = cur.advance().lexeme();
                    }
                    else {
                        return error("Expected operator after USING", cur.fullPos());
                    }
            }
        }

        if (expr != null) {
            return ok(OrderItem.of(expr, null, direction, nulls, collateName, usingOperator));
        }
        return ok(OrderItem.of(null, ordinal, direction, nulls, collateName, usingOperator));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<OrderItem> targetType() {
        return OrderItem.class;
    }
}
