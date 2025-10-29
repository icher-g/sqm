package io.sqm.render.ansi;

import io.sqm.core.Direction;
import io.sqm.core.Nulls;
import io.sqm.core.OrderItem;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

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
        // expr (column or function) — delegate to its renderer
        w.append(node.expr());

        // COLLATE (if any) — typically placed right after the expr
        var collate = node.collate();
        if (collate != null && !collate.isBlank()) {
            var quoter = ctx.dialect().quoter();
            w.space().append("COLLATE").space().append(quoter.quoteIfNeeded(collate));
        }

        // ASC/DESC (omit if unspecified -> dialect default)
        var dir = node.direction();
        if (dir != null) {
            w.space().append(dir == Direction.ASC ? "ASC" : "DESC");
        }

        // NULLS FIRST/LAST/DEFAULT (only when dialect supports explicit keywords)
        var ns = ctx.dialect().nullSorting();
        var n = node.nulls();
        if (n != null && ns != null && ns.supportsExplicit()) {
            // Map DEFAULT to dialect’s default for the (possibly null) direction
            if (n == Nulls.DEFAULT) {
                var effectiveDir = (dir != null) ? dir : Direction.ASC;
                n = ns.defaultFor(effectiveDir);
            }
            var keyword = ns.keyword(n); // e.g., "NULLS FIRST" / "NULLS LAST"
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
