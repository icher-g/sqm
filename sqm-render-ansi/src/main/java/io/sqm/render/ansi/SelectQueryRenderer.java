package io.sqm.render.ansi;

import io.sqm.core.SelectQuery;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders SELECT query statements.
 */
public class SelectQueryRenderer implements Renderer<SelectQuery> {

    private final LimitOffsetRenderer limitOffsetRenderer = new LimitOffsetRenderer();

    /**
     * Creates a SELECT-query renderer.
     */
    public SelectQueryRenderer() {
    }

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
        if (node.distinct() != null) {
            w.space().append(node.distinct());
        }

        var ps = ctx.dialect().paginationStyle();
        var limitOffset = node.limitOffset();

        // Inject TOP n into the head if dialect supports TOP and there's no OFFSET
        if (ps.supportsTop()
            && limitOffset != null
            && limitOffset.limit() != null
            && !limitOffset.limitAll()
            && limitOffset.offset() == null) {
            w.space().append("TOP ");
            w.append(limitOffset.limit());
        }

        w.space();
        w.comma(node.items()); // each column rendered via its own registered renderer

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

        // Pagination tail — pick the right style
        if (limitOffset != null) {
            limitOffsetRenderer.render(limitOffset, ctx, w);
        }

        // Locking clause (FOR UPDATE, FOR SHARE, etc.)
        if (node.lockFor() != null) {
            w.newline().append(node.lockFor());
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
