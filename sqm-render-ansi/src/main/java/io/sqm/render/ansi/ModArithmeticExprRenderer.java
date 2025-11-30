package io.sqm.render.ansi;

import io.sqm.core.AdditiveArithmeticExpr;
import io.sqm.core.Expression;
import io.sqm.core.ModArithmeticExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class ModArithmeticExprRenderer implements Renderer<ModArithmeticExpr> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(ModArithmeticExpr node, RenderContext ctx, SqlWriter w) {
        w.append("MOD").append("(");
        w.append(node.lhs(), enclose(node.lhs()));
        w.append(",").space();
        w.append(node.rhs(), enclose(node.lhs()));
        w.append(")");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<ModArithmeticExpr> targetType() {
        return ModArithmeticExpr.class;
    }

    private boolean enclose(Expression expr) {
        return expr instanceof AdditiveArithmeticExpr && !(expr instanceof ModArithmeticExpr);
    }
}
