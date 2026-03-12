package io.sqm.render.mysql;

import io.sqm.core.ConcatExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders concatenation expressions using MySQL {@code CONCAT(...)} syntax.
 */
public class MySqlConcatExprRenderer implements Renderer<ConcatExpr> {

    /**
     * Creates a MySQL concatenation-expression renderer.
     */
    public MySqlConcatExprRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(ConcatExpr node, RenderContext ctx, SqlWriter w) {
        w.append("CONCAT(").comma(node.args()).append(")");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends ConcatExpr> targetType() {
        return ConcatExpr.class;
    }
}
