package io.sqm.render.oracle;

import io.sqm.core.PriorExpr;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle {@code PRIOR} expressions.
 */
public class PriorExprRenderer extends io.sqm.render.ansi.PriorExprRenderer {
    /**
     * Creates an Oracle prior-expression renderer.
     */
    public PriorExprRenderer() {
    }

    /**
     * Renders an Oracle {@code PRIOR} expression.
     *
     * @param node prior expression
     * @param ctx render context
     * @param w SQL writer
     */
    @Override
    public void render(PriorExpr node, RenderContext ctx, SqlWriter w) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.HIERARCHICAL_QUERY)) {
            throw new UnsupportedDialectFeatureException(SqlFeature.HIERARCHICAL_QUERY.description(), ctx.dialect().name());
        }
        w.append("PRIOR").space().append(node.expr());
    }
}
