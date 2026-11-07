package io.sqm.render.sqlserver;

import io.sqm.core.CompositeQuery;
import io.sqm.core.Expression;
import io.sqm.core.LimitOffset;
import io.sqm.core.Query;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders SQL Server set results with ordered OFFSET/FETCH pagination.
 */
public class CompositeQueryRenderer extends io.sqm.render.ansi.CompositeQueryRenderer {
    /**
     * Creates a SQL Server composite-query renderer.
     */
    public CompositeQueryRenderer() {
    }

    /**
     * Renders a set result, supplying zero OFFSET for limit-only pagination.
     *
     * @param node composite query
     * @param ctx dialect render context
     * @param w SQL writer
     */
    @Override
    public void render(CompositeQuery node, RenderContext ctx, SqlWriter w) {
        var pagination = node.limitOffset();
        if (pagination != null) {
            if (pagination.limitAll()) {
                throw new UnsupportedOperationException("LIMIT ALL is not supported by SQL Server");
            }
            if (pagination.limit() != null || pagination.offset() != null) {
                if (node.orderBy() == null) {
                    throw new UnsupportedOperationException("OFFSET/FETCH requires ORDER BY in SQL Server");
                }
                if (pagination.offset() == null) {
                    node = Query.compose(node.terms(), node.ops(), node.orderBy(), LimitOffset.of(pagination.limit(), Expression.literal(0L)));
                }
            }
        }
        super.render(node, ctx, w);
    }
}
