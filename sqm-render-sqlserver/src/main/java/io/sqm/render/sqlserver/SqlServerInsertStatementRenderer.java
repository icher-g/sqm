package io.sqm.render.sqlserver;

import io.sqm.core.OutputClause;
import io.sqm.core.OutputColumnExpr;
import io.sqm.core.OutputRowSource;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.ansi.InsertStatementRenderer;
import io.sqm.render.spi.RenderContext;

/**
 * Renders SQL Server {@code INSERT} statements with {@code OUTPUT}.
 */
public class SqlServerInsertStatementRenderer extends InsertStatementRenderer {

    /**
     * Creates a SQL Server insert renderer.
     */
    public SqlServerInsertStatementRenderer() {
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
            if (item.expression() instanceof OutputColumnExpr outputColumn && outputColumn.source() == OutputRowSource.DELETED) {
                throw new UnsupportedDialectFeatureException("INSERT ... OUTPUT deleted.<column>", ctx.dialect().name());
            }
        }

        w.space().append(output);
    }
}