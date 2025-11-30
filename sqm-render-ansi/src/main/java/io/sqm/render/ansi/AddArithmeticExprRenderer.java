package io.sqm.render.ansi;

import io.sqm.core.AddArithmeticExpr;
import io.sqm.core.AdditiveArithmeticExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class AddArithmeticExprRenderer implements Renderer<AddArithmeticExpr> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(AddArithmeticExpr node, RenderContext ctx, SqlWriter w) {
        w.append(node.lhs(), node.lhs() instanceof AdditiveArithmeticExpr);
        w.space().append(ctx.dialect().operators().add()).space();
        w.append(node.rhs(), node.rhs() instanceof AdditiveArithmeticExpr);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<AddArithmeticExpr> targetType() {
        return AddArithmeticExpr.class;
    }
}
