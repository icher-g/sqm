package io.sqm.render.ansi;

import io.sqm.core.LimitOffset;
import io.sqm.core.SelectQuery;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class SelectQueryRenderer implements Renderer<SelectQuery> {

    private final LimitOffsetRenderer limitOffsetRenderer = new LimitOffsetRenderer();

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(SelectQuery node, RenderContext ctx, SqlWriter w) {
        // SELECT
        w.append("SELECT");
        if (node.distinct() != null && node.distinct()) {
            w.space().append("DISTINCT");
        }

        var ps = ctx.dialect().paginationStyle();

        // Inject TOP n into the head if dialect supports TOP and there's no OFFSET
        if (ps.supportsTop() && node.limit() != null && node.offset() == null) {
            w.space().append("TOP ").append(node.limit().toString());
        }

        w.space();
        w.comma(node.select()); // each column rendered via its own registered renderer

        // FROM
        if (node.from() != null) {
            w.newline().append("FROM").space();
            w.append(node.from()); // table (with optional schema/alias) rendered by its renderer
        }

        // JOINS
        if (node.joins() != null) {
            for (var j : node.joins()) {
                w.newline();
                w.append(j); // each join rendered by AnsiTableJoinRenderer (or dialect-specific)
            }
        }

        // WHERE
        if (node.where() != null) {
            w.newline().append("WHERE").space();
            w.append(node.where()); // delegate to filter renderer (composite/single/etc.)
        }

        // GROUP BY
        if (node.groupBy() != null) {
            w.newline().append(node.groupBy());
        }

        // HAVING
        if (node.having() != null) {
            w.newline().append("HAVING").space();
            w.append(node.having());
        }

        // WINDOW
        if (node.windows() != null) {
            for (var window : node.windows()) {
                w.newline().append(window);
            }
        }

        // ORDER BY
        if (node.orderBy() != null) {
            w.newline().append(node.orderBy());
        }

        Long limit = node.limit();
        Long offset = node.offset();

        // Pagination tail — pick the right style
        if (limit != null || offset != null) {
            limitOffsetRenderer.render(LimitOffset.of(limit, offset), ctx, w);
        }
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<SelectQuery> targetType() {
        return SelectQuery.class;
    }
}
