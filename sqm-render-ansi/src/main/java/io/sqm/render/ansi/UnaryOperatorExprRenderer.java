package io.sqm.render.ansi;

import io.sqm.core.UnaryOperatorExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class UnaryOperatorExprRenderer implements Renderer<UnaryOperatorExpr> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(UnaryOperatorExpr node, RenderContext ctx, SqlWriter w) {
        var operatorName = node.operator();
        if (operatorName.operatorKeywordSyntax()) {
            throw new IllegalStateException("Unary OPERATOR(...) syntax is not supported");
        }
        w.append(operatorName.symbol()).append(node.expr());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends UnaryOperatorExpr> targetType() {
        return UnaryOperatorExpr.class;
    }
}
