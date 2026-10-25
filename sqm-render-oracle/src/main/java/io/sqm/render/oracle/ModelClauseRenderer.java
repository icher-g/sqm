package io.sqm.render.oracle;

import io.sqm.core.ModelClause;
import io.sqm.core.ReturnRows;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders basic Oracle MODEL ModelClause syntax through registered child renderers.
 */
public class ModelClauseRenderer extends io.sqm.render.ansi.ModelClauseRenderer {
    /**
     * Creates an Oracle ModelClause renderer.
     */
    public ModelClauseRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(ModelClause node, RenderContext ctx, SqlWriter w) {
        ModelRenderSupport.require(ctx);
        if (!node.references().isEmpty()) {
            // The registered reference-model renderer supplies the #506 diagnostic.
            w.append(node.references().getFirst());
        }
        w.append("MODEL");
        if (node.returnRows() == ReturnRows.UPDATED) w.space().append("RETURN UPDATED ROWS");
        w.newline().indent().append(node.main()).outdent();
    }
}
