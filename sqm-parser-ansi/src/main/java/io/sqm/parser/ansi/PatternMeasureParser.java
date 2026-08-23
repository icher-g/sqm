package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.PatternMeasure;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses a named expression in a {@code MEASURES} clause.
 */
public class PatternMeasureParser implements Parser<PatternMeasure> {
    /**
     * Creates a pattern-measure parser.
     */
    public PatternMeasureParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends PatternMeasure> parse(Cursor cur, ParseContext ctx) {
        var expression = ctx.parse(Expression.class, cur);
        if (expression.isError()) return error(expression);
        cur.consumeIf(TokenType.AS);
        var alias = toIdentifier(cur.expect("Expected measure alias", TokenType.IDENT));
        return ok(PatternMeasure.of(expression.value(), alias));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PatternMeasure> targetType() {
        return PatternMeasure.class;
    }
}
