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
        w.append("MODEL");
        if (node.returnRows() == ReturnRows.UPDATED) w.space().append("RETURN UPDATED ROWS");
        w.newline().indent();
        for (var reference : node.references()) w.append(reference).newline();
        w.append(node.main()).outdent();
    }
}
