package io.sqm.render.ansi;

import io.sqm.core.PowerArithmeticExpr;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class PowerArithmeticExprRenderer implements Renderer<PowerArithmeticExpr> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(PowerArithmeticExpr node, RenderContext ctx, SqlWriter w) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.EXPONENTIATION_OPERATOR)) {
            throw new UnsupportedDialectFeatureException("Exponentiation operator", ctx.dialect().name());
        }
        w.append(node.lhs()).space().append("^").space().append(node.rhs());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends PowerArithmeticExpr> targetType() {
        return PowerArithmeticExpr.class;
    }
}
