package io.sqm.render.postgresql;

import io.sqm.core.Direction;
import io.sqm.core.Nulls;
import io.sqm.core.OrderItem;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * PostgreSQL renderer for ORDER BY items supporting {@code USING <operator>}.
 */
public class OrderItemRenderer implements Renderer<OrderItem> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(OrderItem node, RenderContext ctx, SqlWriter w) {
        if (node.expr() != null) {
            w.append(node.expr());
        }
        else {
            w.append(String.valueOf(node.ordinal()));
        }

        var collate = node.collate();
        if (collate != null && !collate.isBlank()) {
            var quoter = ctx.dialect().quoter();
            w.space().append("COLLATE").space().append(quoter.quoteIfNeeded(collate));
        }

        var usingOperator = node.usingOperator();
        if (usingOperator != null && !usingOperator.isBlank()) {
            w.space().append("USING").space().append(usingOperator);
        }

        var dir = node.direction();
        if (dir != null) {
            w.space().append(dir == Direction.ASC ? "ASC" : "DESC");
        }

        var ns = ctx.dialect().nullSorting();
        var n = node.nulls();
        if (n != null && ns != null && ns.supportsExplicit()) {
            if (n == Nulls.DEFAULT) {
                var effectiveDir = (dir != null) ? dir : Direction.ASC;
                n = ns.defaultFor(effectiveDir);
            }
            var keyword = ns.keyword(n);
            if (keyword != null && !keyword.isBlank()) {
                w.space().append(keyword);
            }
        }
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<OrderItem> targetType() {
        return OrderItem.class;
    }
}
