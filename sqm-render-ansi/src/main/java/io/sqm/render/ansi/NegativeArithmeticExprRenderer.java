package io.sqm.render.ansi;

import io.sqm.core.ArithmeticExpr;
import io.sqm.core.NegativeArithmeticExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders unary negative arithmetic expressions.
 */
public class NegativeArithmeticExprRenderer implements Renderer<NegativeArithmeticExpr> {
    /**
     * Creates a negative-arithmetic renderer.
     */
    public NegativeArithmeticExprRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(NegativeArithmeticExpr node, RenderContext ctx, SqlWriter w) {
        w.append(ctx.dialect().operators().neg()).append(node.expr(), node.expr() instanceof ArithmeticExpr);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<NegativeArithmeticExpr> targetType() {
        return NegativeArithmeticExpr.class;
    }
}
