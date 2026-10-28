package io.sqm.parser.oracle;

import io.sqm.core.CellSelector;
import io.sqm.core.Expression;
import io.sqm.core.FunctionExpr;
import io.sqm.core.ModelAggregateExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

/**
 * Parses SUM(sales)[product = 'Bike', year_key BETWEEN 2025 AND 2027], including postfix dispatch.
 */
public class ModelAggregateExprParser extends io.sqm.parser.ansi.ModelAggregateExprParser
    implements InfixParser<Expression, ModelAggregateExpr> {
    /**
     * Creates an aggregate-cell parser.
     */
    public ModelAggregateExprParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends ModelAggregateExpr> parse(Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        return parse(ModelGrammar.node(FunctionExpr.class, cur, ctx), cur, ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<ModelAggregateExpr> parse(Expression lhs, Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        if (!(lhs instanceof FunctionExpr aggregate)) {
            return ParseResult.error("Expected aggregate invocation before MODEL selectors", cur.fullPos());
        }
        var selectors = ModelGrammar.list(this, CellSelector.class, cur, ctx, TokenType.LBRACKET, TokenType.RBRACKET);
        return ParseResult.ok(ModelAggregateExpr.of(aggregate, selectors));
    }
}
