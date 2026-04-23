package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.Lookahead;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses SQL function expressions.
 */
@SuppressWarnings("unused")
public class FunctionExprParser implements MatchableParser<FunctionExpr> {
    /**
     * Creates a function expression parser.
     */
    public FunctionExprParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<FunctionExpr> parse(Cursor cur, ParseContext ctx) {
        var functionName = parseFunctionName(cur);

        cur.expect("Expected '(' after function name", TokenType.LPAREN);

        var distinct = parseDistinct(cur, ctx, functionName);
        var args = parseArguments(cur, ctx, functionName);
        if (args.isError()) {
            return error(args);
        }

        var orderBy = parseOrderBy(cur, ctx, functionName, args.value());
        if (orderBy.isError()) {
            return error(orderBy);
        }

        cur.expect("Expected ')' to close function", TokenType.RPAREN);

        var withinGroup = parseWithinGroup(cur, ctx, functionName);
        if (withinGroup.isError()) {
            return error(withinGroup);
        }

        var filter = parseFilter(cur, ctx, functionName);
        if (filter.isError()) {
            return error(filter);
        }

        var over = parseOver(cur, ctx, functionName);
        if (over.isError()) {
            return error(over);
        }

        return ok(buildFunction(functionName, args.value(), distinct, orderBy.value(), withinGroup.value(), filter.value(), over.value()));
    }

    /**
     * Parses the qualified function name after the leading identifier token.
     *
     * @param cur cursor positioned at the function name.
     * @return parsed qualified function name.
     */
    protected QualifiedName parseFunctionName(Cursor cur) {
        var token = cur.expect("Expected function name", TokenType.IDENT);
        return parseQualifiedName(toIdentifier(token), cur);
    }

    /**
     * Parses the optional {@code DISTINCT} marker inside the argument list.
     *
     * @param cur cursor positioned after the opening parenthesis.
     * @param ctx parser context.
     * @param functionName parsed qualified function name.
     * @return parsed distinct flag or {@code null} when absent.
     */
    protected Boolean parseDistinct(Cursor cur, ParseContext ctx, QualifiedName functionName) {
        return cur.consumeIf(TokenType.DISTINCT) ? true : null;
    }

    /**
     * Parses the function argument list.
     *
     * @param cur cursor positioned at the first argument token or closing parenthesis.
     * @param ctx parser context.
     * @param functionName parsed qualified function name.
     * @return parsed immutable argument list.
     */
    protected ParseResult<? extends List<FunctionExpr.Arg>> parseArguments(
        Cursor cur,
        ParseContext ctx,
        QualifiedName functionName
    ) {
        final List<FunctionExpr.Arg> args = new ArrayList<>();
        if (!cur.match(TokenType.RPAREN)) {
            do {
                var arg = parseArgument(args.size(), cur, ctx, functionName);
                if (arg.isError()) {
                    return error(arg);
                }
                args.add(arg.value());
            } while (cur.consumeIf(TokenType.COMMA));
        }
        return ok(List.copyOf(args));
    }

    /**
     * Parses one function argument.
     *
     * @param index zero-based argument index.
     * @param cur cursor positioned at the argument.
     * @param ctx parser context.
     * @param functionName parsed qualified function name.
     * @return parsed function argument.
     */
    protected ParseResult<? extends FunctionExpr.Arg> parseArgument(
        int index,
        Cursor cur,
        ParseContext ctx,
        QualifiedName functionName
    ) {
        return ctx.parse(FunctionExpr.Arg.class, cur);
    }

    /**
     * Parses optional ordering inside the function argument list.
     *
     * @param cur cursor positioned after parsed arguments.
     * @param ctx parser context.
     * @param functionName parsed qualified function name.
     * @param args parsed function arguments.
     * @return parsed order-by clause or {@code null}.
     */
    protected ParseResult<? extends OrderBy> parseOrderBy(
        Cursor cur,
        ParseContext ctx,
        QualifiedName functionName,
        List<FunctionExpr.Arg> args
    ) {
        return ok(null);
    }

    /**
     * Parses the optional {@code WITHIN GROUP (...)} clause.
     *
     * @param cur cursor positioned after the closing parenthesis of the argument list.
     * @param ctx parser context.
     * @param functionName parsed qualified function name.
     * @return parsed {@code WITHIN GROUP} clause or {@code null}.
     */
    protected ParseResult<? extends OrderBy> parseWithinGroup(
        Cursor cur,
        ParseContext ctx,
        QualifiedName functionName
    ) {
        if (!cur.consumeIf(TokenType.WITHIN)) {
            return ok(null);
        }
        cur.expect("Expected GROUP after WITHIN", TokenType.GROUP);
        cur.expect("Expected '(' after WITHIN GROUP", TokenType.LPAREN);
        var orderBy = ctx.parse(OrderBy.class, cur);
        cur.expect("Expected ')' to close statement", TokenType.RPAREN);
        return orderBy;
    }

    /**
     * Parses the optional {@code FILTER (WHERE ...)} clause.
     *
     * @param cur cursor positioned after the argument list or {@code WITHIN GROUP}.
     * @param ctx parser context.
     * @param functionName parsed qualified function name.
     * @return parsed filter predicate or {@code null}.
     */
    protected ParseResult<? extends Predicate> parseFilter(
        Cursor cur,
        ParseContext ctx,
        QualifiedName functionName
    ) {
        if (!cur.consumeIf(TokenType.FILTER)) {
            return ok(null);
        }
        cur.expect("Expected '(' after FILTER", TokenType.LPAREN);
        cur.expect("Expected WHERE in a FILTER", TokenType.WHERE);
        var filter = ctx.parse(Predicate.class, cur);
        cur.expect("Expected ')' to close statement", TokenType.RPAREN);
        return filter;
    }

    /**
     * Parses the optional {@code OVER ...} clause.
     *
     * @param cur cursor positioned after the argument list and optional aggregate clauses.
     * @param ctx parser context.
     * @param functionName parsed qualified function name.
     * @return parsed over specification or {@code null}.
     */
    protected ParseResult<? extends OverSpec> parseOver(
        Cursor cur,
        ParseContext ctx,
        QualifiedName functionName
    ) {
        if (!cur.match(TokenType.OVER)) {
            return ok(null);
        }
        return ctx.parse(OverSpec.class, cur);
    }

    /**
     * Builds the final function expression from parsed phases.
     *
     * @param functionName parsed qualified function name.
     * @param args parsed arguments.
     * @param distinct optional distinct marker.
     * @param orderBy optional aggregate input ordering.
     * @param withinGroup optional within-group clause.
     * @param filter optional filter clause.
     * @param over optional over clause.
     * @return final function expression.
     */
    protected FunctionExpr buildFunction(
        QualifiedName functionName,
        List<FunctionExpr.Arg> args,
        Boolean distinct,
        OrderBy orderBy,
        OrderBy withinGroup,
        Predicate filter,
        OverSpec over
    ) {
        return FunctionExpr.of(functionName, args, distinct, orderBy, withinGroup, filter, over);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<FunctionExpr> targetType() {
        return FunctionExpr.class;
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
        if (!cur.match(TokenType.IDENT)) {
            return false;
        }
        var p = Lookahead.at(1); // IDENT
        while (cur.match(TokenType.DOT, p.current()) && cur.match(TokenType.IDENT, p.current() + 1)) {
            p.increment(2);
        }
        return cur.match(TokenType.LPAREN, p.current());
    }
}

