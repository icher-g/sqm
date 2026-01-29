package io.sqm.render.postgresql;

import io.sqm.core.ArithmeticExpr;
import io.sqm.core.BinaryOperatorExpr;
import io.sqm.core.CastExpr;
import io.sqm.core.Expression;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class CastExprRenderer implements Renderer<CastExpr> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(CastExpr node, RenderContext ctx, SqlWriter w) {
        w.append(node.expr(), isComposite(node.expr()));
        w.append("::");
        w.append(node.type());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends CastExpr> targetType() {
        return CastExpr.class;
    }

    private boolean isComposite(Expression expr) {
        return expr instanceof ArithmeticExpr || expr instanceof BinaryOperatorExpr;
    }
}
