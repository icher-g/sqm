package io.sqm.render.ansi;

import io.sqm.core.SelectModifier;
import io.sqm.core.SelectQuery;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders SELECT query statements.
 */
public class SelectQueryRenderer implements Renderer<SelectQuery> {

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
        if (!node.optimizerHints().isEmpty()
            && !ctx.dialect().capabilities().supports(SqlFeature.OPTIMIZER_HINT_COMMENT)) {
            throw new UnsupportedDialectFeatureException("optimizer hint comments", ctx.dialect().name());
        }

        if (node.modifiers().contains(SelectModifier.CALC_FOUND_ROWS)
            && !ctx.dialect().capabilities().supports(SqlFeature.CALC_FOUND_ROWS_MODIFIER)) {
            throw new UnsupportedDialectFeatureException("SQL_CALC_FOUND_ROWS", ctx.dialect().name());
        }

        w.append("SELECT");

        renderAfterSelectKeyword(node, ctx, w);

        if (node.distinct() != null) {
            w.space().append(node.distinct());
        }

        var ps = ctx.dialect().paginationStyle();
        var limitOffset = node.limitOffset();

        if (ps.supportsTop()
            && limitOffset != null
            && limitOffset.limit() != null
            && !limitOffset.limitAll()
            && limitOffset.offset() == null) {
            w.space().append("TOP ");
            w.append(limitOffset.limit());
        }

        w.space();
        w.comma(node.items());

        if (node.from() != null) {
            w.newline().append("FROM").space();
            w.append(node.from());
        }

        if (node.joins() != null) {
            for (var j : node.joins()) {
                w.newline();
                w.append(j);
            }
        }

        if (node.where() != null) {
            w.newline().append("WHERE").space();
            w.append(node.where());
        }

        if (node.groupBy() != null) {
            w.newline().append(node.groupBy());
        }

        if (node.having() != null) {
            w.newline().append("HAVING").space();
            w.append(node.having());
        }

        if (node.windows() != null) {
            for (var window : node.windows()) {
                w.newline().append(window);
            }
        }

        if (node.orderBy() != null) {
            w.newline().append(node.orderBy());
        }

        if (limitOffset != null) {
            w.append(limitOffset);
        }

        if (node.lockFor() != null) {
            w.newline().append(node.lockFor());
        }
    }

    /**
     * Hook for dialect-specific rendering right after the {@code SELECT} keyword.
     *
     * @param node query node.
     * @param ctx  render context.
     * @param w    sql writer.
     */
    protected void renderAfterSelectKeyword(SelectQuery node, RenderContext ctx, SqlWriter w) {
        // no-op by default
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
