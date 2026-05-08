package io.sqm.render.oracle;

import io.sqm.core.VariableResultTarget;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders Oracle {@code RETURNING ... INTO} variable targets.
 */
public class VariableResultTargetRenderer implements Renderer<VariableResultTarget> {

    /**
     * Creates a variable result target renderer.
     */
    public VariableResultTargetRenderer() {
    }

    @Override
    public void render(VariableResultTarget node, RenderContext ctx, SqlWriter w) {
        w.comma(node.variables());
    }

    @Override
    public Class<VariableResultTarget> targetType() {
        return VariableResultTarget.class;
    }
}
