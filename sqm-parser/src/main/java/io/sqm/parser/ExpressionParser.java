package io.sqm.parser;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class ExpressionParser implements Parser<Expression> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<Expression> parse(Cursor cur, ParseContext ctx) {

        if (ctx.lookups().looksLikeParam(cur)) {
            var res = ctx.parse(ParamExpr.class, cur);
            return finalize(cur, ctx, res);
        }

        if (ctx.lookups().looksLikeCaseExpr(cur)) {
            var res = ctx.parse(CaseExpr.class, cur);
            return finalize(cur, ctx, res);
        }

        if (ctx.lookups().looksLikeFunctionCall(cur)) {
            var res = ctx.parse(FunctionExpr.class, cur);
            return finalize(cur, ctx, res);
        }

        // row expression expected to start from '('.
        if (ctx.lookups().looksLikeRowExpr(cur)) {
            var res = ctx.parse(RowExpr.class, cur);
            return finalize(cur, ctx, res);
        }

        if (ctx.lookups().looksLikeRowListExpr(cur)) {
            var res = ctx.parse(RowListExpr.class, cur);
            return finalize(cur, ctx, res);
        }

        if (ctx.lookups().looksLikeQueryExpr(cur)) {
            var res = ctx.parse(QueryExpr.class, cur);
            return finalize(cur, ctx, res);
        }

        if (ctx.lookups().looksLikeColumnRef(cur)) {
            var res = ctx.parse(ColumnExpr.class, cur);
            return finalize(cur, ctx, res);
        }

        if (ctx.lookups().looksLikeLiteralExpr(cur)) {
            var res = ctx.parse(LiteralExpr.class, cur);
            return finalize(cur, ctx, res);
        }

        // Predicate & ValueSet should not be used here to avoid recursive calls.
        return error("Unsupported expression token: " + cur.peek().lexeme(), cur.fullPos());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<Expression> targetType() {
        return Expression.class;
    }
}
