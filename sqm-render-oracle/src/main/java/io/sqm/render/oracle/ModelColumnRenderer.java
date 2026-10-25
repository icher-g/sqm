package io.sqm.render.oracle;

import io.sqm.core.ColumnExpr;
import io.sqm.core.ModelColumn;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders basic Oracle MODEL ModelColumn syntax through registered child renderers.
 */
public class ModelColumnRenderer extends io.sqm.render.ansi.ModelColumnRenderer {
    /**
     * Creates an Oracle ModelColumn renderer.
     */
    public ModelColumnRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(ModelColumn node, RenderContext ctx, SqlWriter w) {
        ModelRenderSupport.require(ctx);
        w.append(node.expression());
        if (!(node.expression() instanceof ColumnExpr column) || !column.name().equals(node.name())) {
            w.append(" AS ").append(renderIdentifier(node.name(), ctx.dialect().quoter()));
        }
    }
}
