package io.sqm.parser.ansi;

import io.sqm.core.ArraySliceExpr;
import io.sqm.core.Expression;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.AtomicExprParser;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.*;

import java.util.Set;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses array slice expressions.
 */
public class ArraySliceExprParser implements MatchableParser<ArraySliceExpr>, InfixParser<Expression, ArraySliceExpr> {

    private final AtomicExprParser atomicParser;

    /**
     * Creates an array slice parser.
     *
     * @param atomicParser parser for atomic expressions
     */
    public ArraySliceExprParser(AtomicExprParser atomicParser) {
        this.atomicParser = atomicParser;
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends ArraySliceExpr> parse(Cursor cur, ParseContext ctx) {
        if (!ctx.capabilities().supports(SqlFeature.ARRAY_SLICE)) {
            return error("Array slices are not supported by this dialect", cur.fullPos());
        }
        var left = atomicParser.parse(cur, ctx);
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
    public Class<? extends ArraySliceExpr> targetType() {
        return ArraySliceExpr.class;
    }

    /**
     * Parses a binary operator occurrence where the left-hand side operand
     * has already been parsed.
     *
     * <p>The cursor is positioned at the operator token when this method
     * is invoked. Implementations are responsible for consuming the operator
     * token, parsing the right-hand side operand, and constructing the
     * resulting node.</p>
     *
     * @param lhs the already parsed left-hand operand
     * @param cur the cursor positioned at the operator token
     * @param ctx the parse context
     * @return the parsing result representing {@code lhs <op> rhs}
     */
    @Override
    public ParseResult<ArraySliceExpr> parse(Expression lhs, Cursor cur, ParseContext ctx) {
        if (!ctx.capabilities().supports(SqlFeature.ARRAY_SLICE)) {
            return error("Array slices are not supported by this dialect", cur.fullPos());
        }
        cur.expect("Expected '[' to start array slice", TokenType.LBRACKET);

        Expression from = null;
        Expression to = null;

        if (!cur.match(TokenType.COLON)) {
            var fromExpr = ctx.parse(Expression.class, cur);
            if (fromExpr.isError()) {
                return error(fromExpr);
            }
            from = fromExpr.value();
        }

        cur.expect("Expected ':' inside array slice", TokenType.COLON);

        if (!cur.match(TokenType.RBRACKET)) {
            var toExpr = ctx.parse(Expression.class, cur);
            if (toExpr.isError()) {
                return error(toExpr);
            }
            to = toExpr.value();
        }

        cur.expect("Expected ']' to close array slice", TokenType.RBRACKET);
        return ok(ArraySliceExpr.of(lhs, from, to));
    }

    /**
     * Performs a look-ahead test to determine whether this parser is applicable
     * at the current cursor position.
     * <p>
     * Implementations must <strong>not</strong> advance the cursor or modify
     * the {@link ParseContext}. Their sole responsibility is to inspect the
     * upcoming tokens and decide if this parser is responsible for them.
     *
     * @param cur the cursor pointing at the current token
     * @param ctx the parsing context providing configuration and utilities
     * @return {@code true} if this parser should be used to parse the upcoming
     * input, {@code false} otherwise
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.LBRACKET)) {
            return cur.find(Set.of(TokenType.COLON), Set.of(TokenType.RBRACKET)) < cur.size();
        }
        return false;
    }
}
