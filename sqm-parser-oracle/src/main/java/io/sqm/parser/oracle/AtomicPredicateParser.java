package io.sqm.parser.oracle;

import io.sqm.core.CellPresentPredicate;
import io.sqm.core.Expression;
import io.sqm.core.IsAnyPredicate;
import io.sqm.core.Predicate;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchResult;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

/**
 * Recognizes MODEL predicates such as year_key IS ANY and sales[2025] IS PRESENT.
 */
public class AtomicPredicateParser extends io.sqm.parser.AtomicPredicateParser {
    /**
     * Creates an Oracle atomic-predicate parser.
     */
    public AtomicPredicateParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean canFinishGroupedPredicate(Cursor cur, ParseContext ctx) {
        return super.canFinishGroupedPredicate(cur, ctx)
            || ModelGrammar.inRules(ctx) && cur.matchAny(TokenType.COMMA, TokenType.RBRACKET, TokenType.THEN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected MatchResult<? extends Predicate> parseDialectPredicate(Expression lhs, Cursor cur, ParseContext ctx) {
        if (!cur.match(TokenType.IS)) return MatchResult.notMatched();
        Class<? extends Predicate> type = cur.match(TokenType.ANY, 1)
            ? IsAnyPredicate.class
            : cur.match(TokenType.PRESENT, 1) ? CellPresentPredicate.class : null;
        if (type == null) return MatchResult.notMatched();
        if (!ModelGrammar.inRules(ctx)) {
            return MatchResult.matched(ParseResult.error("MODEL wildcard/presence predicates require MODEL rules", cur.fullPos()));
        }
        return MatchResult.matched(ctx.parse(type, lhs, cur));
    }
}
