package io.sqm.render.postgresql;

import io.sqm.core.ResultClause;
import io.sqm.core.TableRef;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

import java.util.List;

/**
 * Renders PostgreSQL {@code UPDATE} statements, including optional {@code FROM} sources.
 */
public class UpdateStatementRenderer extends io.sqm.render.ansi.UpdateStatementRenderer {

    /**
     * Creates a PostgreSQL update-statement renderer.
     */
    public UpdateStatementRenderer() {
    }

    /**
     * Renders optional PostgreSQL {@code FROM} sources.
     *
     * @param from from sources
     * @param ctx  render context
     * @param w    SQL writer
     */
    @Override
    protected void renderFrom(List<TableRef> from, RenderContext ctx, SqlWriter w) {
        if (from.isEmpty()) {
            return;
        }
        if (!ctx.dialect().capabilities().supports(SqlFeature.UPDATE_FROM)) {
            throw new UnsupportedDialectFeatureException("UPDATE ... FROM", ctx.dialect().name());
        }
        w.newline().append("FROM").space().comma(from);
    }

    /**
     * Renders optional PostgreSQL {@code RETURNING} clause.
     *
     * @param result returning projection items
     * @param ctx    render context
     * @param w      SQL writer
     */
    @Override
    protected void renderReturning(ResultClause result, RenderContext ctx, SqlWriter w) {
        if (result == null || result.items().isEmpty()) {
            return;
        }
        if (!ctx.dialect().capabilities().supports(SqlFeature.DML_RESULT_CLAUSE)) {
            throw new UnsupportedDialectFeatureException("UPDATE ... RETURNING", ctx.dialect().name());
        }
        w.newline().append("RETURNING").space().comma(result.items());
    }

    @Override
    protected void renderOutput(ResultClause result, RenderContext ctx, SqlWriter w) {
        // noop
    }
}
