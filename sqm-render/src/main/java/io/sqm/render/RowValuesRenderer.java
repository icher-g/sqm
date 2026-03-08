package io.sqm.render;

import io.sqm.core.RowExpr;
import io.sqm.core.RowListExpr;
import io.sqm.core.RowValues;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Entry-point renderer for row-values nodes.
 */
public class RowValuesRenderer implements Renderer<RowValues> {

    /**
     * Creates a row-values renderer.
     */
    public RowValuesRenderer() {
    }

    /**
     * Renders row-values nodes by delegating to concrete row renderers.
     *
     * @param node row-values node to render
     * @param ctx rendering context
     * @param w SQL writer
     */
    @Override
    public void render(RowValues node, RenderContext ctx, SqlWriter w) {
        if (node instanceof RowExpr rowExpr) {
            w.append(rowExpr, true);
            return;
        }
        if (node instanceof RowListExpr rowListExpr) {
            w.append(rowListExpr);
            return;
        }
        throw new IllegalArgumentException("Unsupported row-values type: " + node.getClass().getName());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return row-values root type
     */
    @Override
    public Class<RowValues> targetType() {
        return RowValues.class;
    }
}
