package io.sqm.render.postgresql;

import io.sqm.core.*;
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

    private static boolean isEmpty(ResultClause result) {
        return result == null || result.items().isEmpty();
    }

    /**
     * Renders PostgreSQL CTE body.
     *
     * @param body CTE body statement.
     * @param ctx  render context.
     * @param w    SQL writer.
     */
    @Override
    protected void renderBody(Statement body, RenderContext ctx, SqlWriter w) {
        if (body instanceof Query query) {
            w.append(query, true, true);
            return;
        }

        if (!ctx.dialect().capabilities().supports(SqlFeature.DML_RESULT_CLAUSE)) {
            throw new UnsupportedDialectFeatureException("Writable CTE DML RETURNING", ctx.dialect().name());
        }

        if (body instanceof InsertStatement insert) {
            if (isEmpty(insert.result())) {
                throw new IllegalArgumentException("Writable CTE INSERT requires RETURNING");
            }
            w.append(insert, true, true);
            return;
        }

        if (body instanceof UpdateStatement update) {
            if (isEmpty(update.result())) {
                throw new IllegalArgumentException("Writable CTE UPDATE requires RETURNING");
            }
            w.append(update, true, true);
            return;
        }

        if (body instanceof DeleteStatement delete) {
            if (isEmpty(delete.result())) {
                throw new IllegalArgumentException("Writable CTE DELETE requires RETURNING");
            }
            w.append(delete, true, true);
            return;
        }

        throw new UnsupportedDialectFeatureException("Writable CTE body", ctx.dialect().name());
    }
}
