package io.sqm.render.postgresql;

import io.sqm.core.TableRef;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

import java.util.List;

/**
 * Renders PostgreSQL {@code DELETE} statements, including optional {@code USING} sources.
 */
public class DeleteStatementRenderer extends io.sqm.render.ansi.DeleteStatementRenderer {

    /**
     * Creates a PostgreSQL delete-statement renderer.
     */
    public DeleteStatementRenderer() {
    }

    /**
     * Renders optional PostgreSQL {@code USING} sources.
     *
     * @param using using sources
     * @param ctx   render context
     * @param w     SQL writer
     */
    @Override
    protected void renderUsing(List<TableRef> using, RenderContext ctx, SqlWriter w) {
        if (using.isEmpty()) {
            return;
        }
        if (!ctx.dialect().capabilities().supports(SqlFeature.DELETE_USING)) {
            throw new UnsupportedDialectFeatureException("DELETE ... USING", ctx.dialect().name());
        }
        w.space().append("USING").space().comma(using);
    }
}
