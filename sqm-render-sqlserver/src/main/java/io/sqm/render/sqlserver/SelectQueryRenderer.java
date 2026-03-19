package io.sqm.render.sqlserver;

import io.sqm.core.SelectQuery;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders SQL Server SELECT query statements.
 */
public class SelectQueryRenderer extends io.sqm.render.ansi.SelectQueryRenderer {

    /**
     * Creates a SQL Server SELECT-query renderer.
     */
    public SelectQueryRenderer() {
    }

    /**
     * Validates SQL Server SELECT rendering rules.
     *
     * @param node query node.
     * @param ctx render context.
     */
    @Override
    protected void validateQuery(SelectQuery node, RenderContext ctx) {
        super.validateQuery(node, ctx);

        var limitOffset = node.limitOffset();
        if (limitOffset != null && limitOffset.offset() != null && node.orderBy() == null) {
            throw new UnsupportedOperationException("SQL Server OFFSET/FETCH requires ORDER BY");
        }

        var topSpec = node.topSpec();
        if (topSpec != null && topSpec.withTies() && node.orderBy() == null) {
            throw new UnsupportedOperationException("SQL Server TOP WITH TIES requires ORDER BY");
        }
    }

    /**
     * Renders SQL Server {@code TOP (...)} after DISTINCT and before the projection list.
     *
     * @param node query node.
     * @param ctx render context.
     * @param w sql writer.
     */
    @Override
    protected void renderAfterDistinctClause(SelectQuery node, RenderContext ctx, SqlWriter w) {
        var topSpec = node.topSpec();
        if (topSpec != null) {
            w.space().append("TOP (");
            w.append(topSpec.count());
            w.append(")");
            if (topSpec.percent()) {
                w.space().append("PERCENT");
            }
            if (topSpec.withTies()) {
                w.space().append("WITH TIES");
            }
        }
    }
}
