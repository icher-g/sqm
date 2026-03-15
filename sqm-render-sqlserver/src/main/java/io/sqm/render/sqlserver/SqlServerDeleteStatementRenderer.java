package io.sqm.render.sqlserver;

import io.sqm.core.OutputClause;
import io.sqm.core.OutputColumnExpr;
import io.sqm.core.OutputRowSource;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.ansi.DeleteStatementRenderer;
import io.sqm.render.spi.RenderContext;

/**
 * Renders SQL Server {@code DELETE} statements with {@code OUTPUT}.
 */
public class SqlServerDeleteStatementRenderer extends DeleteStatementRenderer {

    /**
     * Creates a SQL Server delete renderer.
     */
    public SqlServerDeleteStatementRenderer() {
    }

    /**
     * Renders optional {@code OUTPUT} clause.
     *
     * @param output output clause
     * @param ctx    render context
     * @param w      SQL writer
     */
    @Override
    protected void renderOutput(OutputClause output, RenderContext ctx, SqlWriter w) {
        if (output == null) {
            return;
        }

        for (var item : output.items()) {
            if (item.expression() instanceof OutputColumnExpr outputColumn && outputColumn.source() == OutputRowSource.INSERTED) {
                throw new UnsupportedDialectFeatureException("DELETE ... OUTPUT inserted.<column>", ctx.dialect().name());
            }
        }

        w.space().append(output);
    }
}