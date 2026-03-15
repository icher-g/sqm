package io.sqm.render.sqlserver;

import io.sqm.core.OutputClause;
import io.sqm.render.SqlWriter;
import io.sqm.render.ansi.UpdateStatementRenderer;
import io.sqm.render.spi.RenderContext;

/**
 * Renders SQL Server {@code UPDATE} statements with {@code OUTPUT}.
 */
public class SqlServerUpdateStatementRenderer extends UpdateStatementRenderer {

    /**
     * Creates a SQL Server update renderer.
     */
    public SqlServerUpdateStatementRenderer() {
    }

    /**
     * Renders optional {@code OUTPUT} clause.
     *
     * @param output output clause
     * @param ctx render context
     * @param w SQL writer
     */
    @Override
    protected void renderOutput(OutputClause output, RenderContext ctx, SqlWriter w) {
        if (output != null) {
            w.space().append(output);
        }
    }
}