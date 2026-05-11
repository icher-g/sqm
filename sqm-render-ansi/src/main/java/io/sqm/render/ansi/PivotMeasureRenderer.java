package io.sqm.render.ansi;

import io.sqm.core.PivotMeasure;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders pivot aggregate measures.
 */
public class PivotMeasureRenderer implements Renderer<PivotMeasure> {
    /**
     * Creates a pivot measure renderer.
     */
    public PivotMeasureRenderer() {
    }

    /**
     * Renders the node into a SQL writer.
     *
     * @param node pivot measure
     * @param ctx render context
     * @param w SQL writer
     */
    @Override
    public void render(PivotMeasure node, RenderContext ctx, SqlWriter w) {
        w.append(node.aggregateFunction());
        if (node.alias() != null) {
            w.space().append("AS").space().append(renderIdentifier(node.alias(), ctx.dialect().quoter()));
        }
    }

    /**
     * Gets the renderer target type.
     *
     * @return pivot measure type
     */
    @Override
    public Class<PivotMeasure> targetType() {
        return PivotMeasure.class;
    }
}
