package io.sqm.render.postgresql;

import io.sqm.core.SelectItem;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

import java.util.List;

/**
 * Renders PostgreSQL {@code INSERT} statements, including optional {@code RETURNING}.
 */
public class InsertStatementRenderer extends io.sqm.render.ansi.InsertStatementRenderer {

    /**
     * Creates a PostgreSQL insert-statement renderer.
     */
    public InsertStatementRenderer() {
    }

    /**
     * Renders optional PostgreSQL {@code RETURNING} clause.
     *
     * @param returning returning projection items
     * @param ctx       render context
     * @param w         SQL writer
     */
    @Override
    protected void renderReturning(List<SelectItem> returning, RenderContext ctx, SqlWriter w) {
        if (returning.isEmpty()) {
            return;
        }
        if (!ctx.dialect().capabilities().supports(SqlFeature.DML_RETURNING)) {
            throw new UnsupportedDialectFeatureException("INSERT ... RETURNING", ctx.dialect().name());
        }
        w.space().append("RETURNING").space().comma(returning);
    }
}
