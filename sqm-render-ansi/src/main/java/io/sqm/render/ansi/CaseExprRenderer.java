package io.sqm.render.ansi;

import io.sqm.core.CaseExpr;
import io.sqm.core.WhenThen;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders CASE expressions.
 */
public class CaseExprRenderer implements Renderer<CaseExpr> {
    /**
     * Creates a CASE-expression renderer.
     */
    public CaseExprRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(CaseExpr node, RenderContext ctx, SqlWriter w) {
        w.append("CASE");

        for (WhenThen arm : node.whens()) {
            w.append(arm);
        }

        if (node.elseExpr() != null) {
            w.space().append("ELSE").space();
            w.append(node.elseExpr());
        }

        w.space().append("END");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<CaseExpr> targetType() {
        return CaseExpr.class;
    }
}
