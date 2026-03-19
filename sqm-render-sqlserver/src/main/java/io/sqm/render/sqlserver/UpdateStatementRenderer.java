package io.sqm.render.sqlserver;

import io.sqm.core.ResultClause;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders SQL Server {@code UPDATE} statements with {@code OUTPUT}.
 */
public class UpdateStatementRenderer extends io.sqm.render.ansi.UpdateStatementRenderer {

    /**
     * Creates a SQL Server update renderer.
     */
    public UpdateStatementRenderer() {
    }

    /**
     * Renders optional {@code OUTPUT} clause.
     *
     * @param result result clause
     * @param ctx render context
     * @param w SQL writer
     */
    @Override
    protected void renderOutput(ResultClause result, RenderContext ctx, SqlWriter w) {
        if (result != null) {
            w.space().append(result);
        }
    }

    @Override
    protected void renderReturning(ResultClause result, RenderContext ctx, SqlWriter w) {
        // noop
    }
}