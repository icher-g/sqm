package io.sqm.render.oracle;

import io.sqm.core.CellFor;
import io.sqm.core.QueryExpr;
import io.sqm.core.RowListExpr;
import io.sqm.core.oracle.internal.ModelSyntaxChecker;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle MODEL syntax such as {@code FOR (product, year_key) IN (('Bike', 2026), ('Car', 2027))} through registered child renderers.
 */
public class CellForValuesRenderer extends io.sqm.render.ansi.CellForValuesRenderer {
    /**
     * Creates an Oracle CellFor.Values renderer.
     */
    public CellForValuesRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(CellFor.Values node, RenderContext ctx, SqlWriter w) {
        ModelRenderSupport.require(ctx);
        ModelSyntaxChecker.values(node);
        w.append("FOR ");
        if (node.dimensions().size() == 1 && !(node.values() instanceof RowListExpr)) {
            w.append(renderIdentifier(node.dimensions().getFirst(), ctx.dialect().quoter()));
        }
        else {
            w.append("(").comma(node.dimensions(), ctx.dialect().quoter()).append(")");
        }
        w.append(" IN ");
        // QueryExpr owns its parentheses; row sources need their enclosing list here.
        if (node.values() instanceof QueryExpr) w.append(node.values());
        else w.append(node.values(), true);
    }
}
