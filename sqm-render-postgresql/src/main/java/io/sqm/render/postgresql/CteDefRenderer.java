package io.sqm.render.postgresql;

import io.sqm.core.DeleteStatement;
import io.sqm.core.InsertStatement;
import io.sqm.core.Query;
import io.sqm.core.Statement;
import io.sqm.core.UpdateStatement;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders PostgreSQL CTE definitions including writable CTE bodies.
 */
public class CteDefRenderer extends io.sqm.render.ansi.CteDefRenderer {

    /**
     * Creates a PostgreSQL CTE renderer.
     */
    public CteDefRenderer() {
    }

    /**
     * Renders PostgreSQL CTE body.
     *
     * @param body CTE body statement.
     * @param ctx render context.
     * @param w SQL writer.
     */
    @Override
    protected void renderBody(Statement body, RenderContext ctx, SqlWriter w) {
        if (body instanceof Query query) {
            w.append(query, true, true);
            return;
        }

        if (!ctx.dialect().capabilities().supports(SqlFeature.DML_RETURNING)) {
            throw new UnsupportedDialectFeatureException("Writable CTE DML RETURNING", ctx.dialect().name());
        }

        if (body instanceof InsertStatement insert) {
            if (insert.returning().isEmpty()) {
                throw new IllegalArgumentException("Writable CTE INSERT requires RETURNING");
            }
            w.append(insert, true, true);
            return;
        }

        if (body instanceof UpdateStatement update) {
            if (update.returning().isEmpty()) {
                throw new IllegalArgumentException("Writable CTE UPDATE requires RETURNING");
            }
            w.append(update, true, true);
            return;
        }

        if (body instanceof DeleteStatement delete) {
            if (delete.returning().isEmpty()) {
                throw new IllegalArgumentException("Writable CTE DELETE requires RETURNING");
            }
            w.append(delete, true, true);
            return;
        }

        throw new UnsupportedDialectFeatureException("Writable CTE body", ctx.dialect().name());
    }
}
