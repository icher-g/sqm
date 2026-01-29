package io.sqm.parser.postgresql;

import io.sqm.core.ArraySubscriptExpr;
import io.sqm.core.Expression;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.Set;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses array subscripting expressions of the form {@code <expr>[<index>]}.
 *
 * <p>This parser is an infix-style parser that requires a pre-parsed left-hand side
 * (the array expression). It is designed to be invoked from the postfix phase of
 * expression parsing, where constructs that bind tighter than binary operators are applied.</p>
 *
 * <p>Examples:</p>
 * <ul>
 *   <li>{@code arr[1]}</li>
 *   <li>{@code arr[i + 1]}</li>
 *   <li>{@code ARRAY[1,2,3][2]}</li>
 *   <li>{@code arr[1][2]} (handled by repeatedly applying this parser)</li>
 * </ul>
 *
 * <p>This parser does not implement PostgreSQL slicing syntax such as {@code arr[2:5]}.
 * If you add a slice node later, it should be parsed either by extending this parser
 * or by introducing a separate parser that also matches {@code '['} and decides between
 * index and slice forms.</p>
 *
 * <p>Expected tokens:</p>
 * <ul>
 *   <li>{@link TokenType#LBRACKET} for {@code '['}</li>
 *   <li>{@link TokenType#RBRACKET} for {@code ']'}</li>
 * </ul>
 *
 * <p>This parser assumes the lexer produces bracket tokens rather than treating
 * bracketed text as a quoted identifier.</p>
 */
public final class ArraySubscriptExprParser implements MatchableParser<ArraySubscriptExpr>, InfixParser<Expression, ArraySubscriptExpr> {

    /**
     * Determines whether an array subscript can be parsed at the current cursor position.
     *
     * <p>This parser matches when the next token is {@code '['}. No input is consumed.</p>
     *
     * @param cur the cursor
     * @param ctx the parse context
     * @return {@code true} if the next token is {@code '['}, otherwise {@code false}
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.LBRACKET)) {
            return cur.find(Set.of(TokenType.COLON), Set.of(TokenType.RBRACKET)) == cur.size();
        }
        return false;
    }

    /**
     * Parses an array subscript using a pre-parsed left-hand side expression.
     *
     * <p>Grammar shape:</p>
     * <pre>
     *   subscript := '[' expression ']'
     * </pre>
     *
     * <p>The returned node is {@link ArraySubscriptExpr#of(Expression, Expression)}.</p>
     *
     * <p>This method consumes the opening bracket, parses the index expression using
     * {@link ParseContext#parse(Class, Cursor)}, and then requires a closing bracket.</p>
     *
     * @param lhs the expression being indexed (left-hand side)
     * @param cur the cursor positioned at the {@code '['} token
     * @param ctx the parse context
     * @return a parse result containing an {@link ArraySubscriptExpr} or an error
     */
    @Override
    public ParseResult<ArraySubscriptExpr> parse(Expression lhs, Cursor cur, ParseContext ctx) {
        cur.expect("Expected '[' to start array subscript", TokenType.LBRACKET);

        ParseResult<? extends Expression> index = ctx.parse(Expression.class, cur);
        if (index.isError()) {
            return error(index);
        }

        cur.expect("Expected ']' to close array subscript", TokenType.RBRACKET);
        return ok(ArraySubscriptExpr.of(lhs, index.value()));
    }

    /**
     * Unsupported entry point for this parser.
     *
     * <p>Array subscripting requires a left-hand side expression and must be parsed
     * via {@link #parse(Expression, Cursor, ParseContext)}. This method returns an
     * error result if invoked.</p>
     *
     * @param cur the cursor
     * @param ctx the parse context
     * @return an error result
     */
    @Override
    public ParseResult<? extends ArraySubscriptExpr> parse(Cursor cur, ParseContext ctx) {
        var left = ctx.parse(Expression.class, cur);
        if (left.isError()) {
            return error(left);
        }
        return parse(left.value(), cur, ctx);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends ArraySubscriptExpr> targetType() {
        return ArraySubscriptExpr.class;
    }
}
