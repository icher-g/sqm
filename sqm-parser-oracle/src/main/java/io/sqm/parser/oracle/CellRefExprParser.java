package io.sqm.parser.oracle;

import io.sqm.core.CellRefExpr;
import io.sqm.core.CellSelector;
import io.sqm.core.ColumnExpr;
import io.sqm.core.Expression;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

/**
 * Parses readable single cells such as sales['Bike', 2025], including postfix dispatch.
 */
public class CellRefExprParser extends io.sqm.parser.ansi.CellRefExprParser implements InfixParser<Expression, CellRefExpr> {
    /**
     * Creates a readable-cell parser.
     */
    public CellRefExprParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CellRefExpr> parse(Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        var column = ModelGrammar.node(ColumnExpr.class, cur, ctx);
        return parse(column, cur, ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<CellRefExpr> parse(Expression lhs, Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        if (!(lhs instanceof ColumnExpr column)) return ParseResult.error("Expected a MODEL measure before [", cur.fullPos());
        if (ctx instanceof ModelParseContext local && local.scope() != null && !local.scope().contains(column.tableAlias(), column.name())) {
            return ParseResult.error("Unknown MODEL measure: " + column.name().value(), cur.fullPos());
        }
        var selectors = ModelGrammar.list(this, CellSelector.class, cur, ctx, TokenType.LBRACKET, TokenType.RBRACKET);
        ModelGrammar.arity(column.tableAlias(), selectors, cur, ctx);
        return ParseResult.ok(CellRefExpr.of(column.tableAlias(), column.name(), selectors));
    }
}
