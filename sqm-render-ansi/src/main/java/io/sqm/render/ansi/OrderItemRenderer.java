package io.sqm.render.ansi;

import io.sqm.core.Direction;
import io.sqm.core.Nulls;
import io.sqm.core.OrderItem;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

import java.util.stream.Collectors;

/**
 * Renders ORDER BY items.
 */
public class OrderItemRenderer implements Renderer<OrderItem> {
    /**
     * Creates an order-item renderer.
     */
    public OrderItemRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(OrderItem node, RenderContext ctx, SqlWriter w) {
        // expr (column or function) or ordinal
        if (node.expr() != null) {
            w.append(node.expr());
        }
        else {
            w.append(String.valueOf(node.ordinal()));
        }

        // COLLATE (if any) - typically placed right after the expr
        var collate = node.collate();
        if (collate != null) {
            var quoter = ctx.dialect().quoter();
            w.space().append("COLLATE").space().append(
                collate.parts().stream()
                    .map(part -> renderIdentifier(part, quoter))
                    .collect(Collectors.joining("."))
            );
        }

        var usingOperator = node.usingOperator();
        if (usingOperator != null && !usingOperator.isBlank()) {
            if (!ctx.dialect().capabilities().supports(SqlFeature.ORDER_BY_USING)) {
                throw new UnsupportedDialectFeatureException("ORDER BY ... USING", ctx.dialect().name());
            }
            w.space().append("USING").space().append(usingOperator);
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
            // Map DEFAULT to dialect's default for the (possibly null) direction
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
