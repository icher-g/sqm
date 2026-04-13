package io.sqm.render.sqlserver;

import io.sqm.core.ExprResultItem;
import io.sqm.core.OutputColumnExpr;
import io.sqm.core.OutputRowSource;
import io.sqm.core.ResultClause;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders SQL Server {@code DELETE} statements with {@code OUTPUT}.
 */
public class DeleteStatementRenderer extends io.sqm.render.ansi.DeleteStatementRenderer {

    /**
     * Creates a SQL Server delete renderer.
     */
    public DeleteStatementRenderer() {
    }

    /**
     * Renders optional {@code OUTPUT} clause.
     *
     * @param result result clause
     * @param ctx    render context
     * @param w      SQL writer
     */
    @Override
    protected void renderOutput(ResultClause result, RenderContext ctx, SqlWriter w) {
        if (result == null) {
            return;
        }

        for (var item : result.items()) {
            if (item instanceof ExprResultItem exprItem && exprItem.expr() instanceof OutputColumnExpr outputColumn && outputColumn.source() == OutputRowSource.INSERTED) {
                throw new UnsupportedDialectFeatureException("DELETE ... OUTPUT inserted.<column>", ctx.dialect().name());
            }
        }

        w.newline().append(result);
    }

    @Override
    protected void renderReturning(ResultClause result, RenderContext ctx, SqlWriter w) {
        // noop
    }
}
