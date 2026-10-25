package io.sqm.parser.oracle;

import io.sqm.core.CellSelector;
import io.sqm.core.Expression;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.OperatorTokens;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

/**
 * Parses a positional value such as 'Bike', 2026, or a scalar expression inside brackets.
 */
public class CellSelectorValueParser extends io.sqm.parser.ansi.CellSelectorValueParser {
    /**
     * Creates a positional-selector parser.
     */
    public CellSelectorValueParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CellSelector.Value> parse(Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        var value = ModelGrammar.node(Expression.class, cur, ctx);
        if (OperatorTokens.isComparison(cur.peek())
            || cur.match(TokenType.IS)
            || cur.match(TokenType.IN)
            || cur.match(TokenType.BETWEEN)
            || cur.match(TokenType.NOT)
            || cur.match(TokenType.LIKE)) {
            return ParseResult.error(ctx.parse(CellSelector.Condition.class, cur));
        }
        return ParseResult.ok(CellSelector.Value.of(value));
    }
}
