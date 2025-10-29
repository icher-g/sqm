package io.sqm.parser;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class PredicateParser implements Parser<Predicate> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<Predicate> parse(Cursor cur, ParseContext ctx) {
        if (ctx.lookups().looksLikeAndPredicate(cur)) {
            var res = ctx.parse(AndPredicate.class, cur);
            return finalize(cur, ctx, res);
        }

        if (ctx.lookups().looksLikeOrPredicate(cur)) {
            var res = ctx.parse(OrPredicate.class, cur);
            return finalize(cur, ctx, res);
        }

        if (ctx.lookups().looksLikeNotPredicate(cur)) {
            var res = ctx.parse(NotPredicate.class, cur);
            return finalize(cur, ctx, res);
        }

        if (ctx.lookups().looksLikeExistsPredicate(cur)) {
            var res = ctx.parse(ExistsPredicate.class, cur);
            return finalize(cur, ctx, res);
        }

        if (ctx.lookups().looksLikeBetweenPredicate(cur)) {
            var res = ctx.parse(BetweenPredicate.class, cur);
            return finalize(cur, ctx, res);
        }

        if (ctx.lookups().looksLikeIsNullPredicate(cur)) {
            var res = ctx.parse(IsNullPredicate.class, cur);
            return finalize(cur, ctx, res);
        }

        if (ctx.lookups().looksLikeInPredicate(cur)) {
            var res = ctx.parse(InPredicate.class, cur);
            return finalize(cur, ctx, res);
        }

        if (ctx.lookups().looksLikeLikePredicate(cur)) {
            var res = ctx.parse(LikePredicate.class, cur);
            return finalize(cur, ctx, res);
        }

        if (ctx.lookups().looksLikeAnyAllPredicate(cur)) {
            var res = ctx.parse(AnyAllPredicate.class, cur);
            return finalize(cur, ctx, res);
        }

        if (ctx.lookups().looksLikeComparisonPredicate(cur)) {
            var res = ctx.parse(ComparisonPredicate.class, cur);
            return finalize(cur, ctx, res);
        }

        if (ctx.lookups().looksLikeUnaryPredicate(cur)) {
            var res = ctx.parse(UnaryPredicate.class, cur);
            return finalize(cur, ctx, res);
        }

        return error("Unsupported predicate token: " + cur.peek().lexeme(), cur.fullPos());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<Predicate> targetType() {
        return Predicate.class;
    }
}
