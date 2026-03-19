package io.sqm.render.sqlserver;

import io.sqm.core.ResultInto;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders SQL Server-style {@code OUTPUT ... INTO ...} targets.
 */
public class ResultIntoRenderer implements Renderer<ResultInto> {

    /**
     * Creates a result-into renderer.
     */
    public ResultIntoRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(ResultInto node, RenderContext ctx, SqlWriter w) {
        w.append("INTO").space().append(node.target());
        if (!node.columns().isEmpty()) {
            w.space().append("(");
            w.comma(node.columns(), ctx.dialect().quoter());
            w.append(")");
        }
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<ResultInto> targetType() {
        return ResultInto.class;
    }
}
