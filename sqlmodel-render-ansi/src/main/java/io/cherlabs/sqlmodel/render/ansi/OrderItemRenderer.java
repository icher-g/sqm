package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.Direction;
import io.cherlabs.sqlmodel.core.Nulls;
import io.cherlabs.sqlmodel.core.OrderItem;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

public class OrderItemRenderer implements Renderer<OrderItem> {
    @Override
    public void render(OrderItem entity, RenderContext ctx, SqlWriter w) {
        // expression (column or function) — delegate to its renderer
        w.append(entity.column());

        // COLLATE (if any) — typically placed right after the expression
        var collate = entity.collate();
        if (collate != null && !collate.isBlank()) {
            var quoter = ctx.dialect().quoter();
            w.space().append("COLLATE").space().append(quoter.quoteIfNeeded(collate));
        }

        // ASC/DESC (omit if unspecified -> dialect default)
        var dir = entity.direction();
        if (dir != null) {
            w.space().append(dir == Direction.ASC ? "ASC" : "DESC");
        }

        // NULLS FIRST/LAST/DEFAULT (only when dialect supports explicit keywords)
        var ns = ctx.dialect().nullSorting();
        var n = entity.nulls();
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
}
