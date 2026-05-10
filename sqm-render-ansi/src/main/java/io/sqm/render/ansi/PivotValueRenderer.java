package io.sqm.render.ansi;

import io.sqm.core.PivotValue;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders explicit pivot values.
 */
public class PivotValueRenderer implements Renderer<PivotValue> {
    /**
     * Creates a pivot value renderer.
     */
    public PivotValueRenderer() {
    }

    /**
     * Renders the node into a SQL writer.
     *
     * @param node pivot value
     * @param ctx render context
     * @param w SQL writer
     */
    @Override
    public void render(PivotValue node, RenderContext ctx, SqlWriter w) {
        w.append(node.value());
        if (node.alias() != null) {
            w.space().append("AS").space().append(renderIdentifier(node.alias(), ctx.dialect().quoter()));
        }
    }

    /**
     * Gets the renderer target type.
     *
     * @return pivot value type
     */
    @Override
    public Class<PivotValue> targetType() {
        return PivotValue.class;
    }
}
