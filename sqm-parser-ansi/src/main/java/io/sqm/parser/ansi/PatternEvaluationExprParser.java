package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.PatternEvaluationExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses a scoped {@code RUNNING} or {@code FINAL} expression wrapper.
 */
public class PatternEvaluationExprParser implements MatchableParser<PatternEvaluationExpr> {
    /**
     * Creates a pattern-evaluation parser.
     */
    public PatternEvaluationExprParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends PatternEvaluationExpr> parse(Cursor cur, ParseContext ctx) {
        var mode = cur.consumeIf(TokenType.RUNNING)
            ? PatternEvaluationExpr.Mode.RUNNING
            : PatternEvaluationExpr.Mode.FINAL;
        if (mode == PatternEvaluationExpr.Mode.FINAL) cur.expect("Expected FINAL", TokenType.FINAL);
        var expression = ctx.parse(Expression.class, cur);
        if (expression.isError()) return error(expression);
        return ok(PatternEvaluationExpr.of(mode, expression.value()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PatternEvaluationExpr> targetType() {
        return PatternEvaluationExpr.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.matchAny(TokenType.RUNNING, TokenType.FINAL);
    }
}
