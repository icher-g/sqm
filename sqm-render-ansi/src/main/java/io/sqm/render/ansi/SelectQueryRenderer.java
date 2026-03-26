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
@SuppressWarnings("unused")
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
        validateQuery(node, ctx);

        w.append("SELECT");

        renderAfterSelectKeyword(node, ctx, w);
        renderDistinctClause(node, ctx, w);
        renderAfterDistinctClause(node, ctx, w);
        renderSelectItems(node, ctx, w);
        renderFromClause(node, ctx, w);
        renderJoins(node, ctx, w);
        renderWhereClause(node, ctx, w);
        renderGroupByClause(node, ctx, w);
        renderHavingClause(node, ctx, w);
        renderWindowClause(node, ctx, w);
        renderOrderByClause(node, ctx, w);
        renderPaginationClause(node, ctx, w);
        renderLockingClause(node, ctx, w);
    }

    /**
     * Validates dialect-specific rendering preconditions.
     *
     * @param node query node.
     * @param ctx  render context.
     */
    protected void validateQuery(SelectQuery node, RenderContext ctx) {
        if (!node.hints().isEmpty()
            && !ctx.dialect().capabilities().supports(SqlFeature.OPTIMIZER_HINT_COMMENT)) {
            throw new UnsupportedDialectFeatureException("optimizer hint comments", ctx.dialect().name());
        }

        if (node.modifiers().contains(SelectModifier.CALC_FOUND_ROWS)
            && !ctx.dialect().capabilities().supports(SqlFeature.CALC_FOUND_ROWS_MODIFIER)) {
            throw new UnsupportedDialectFeatureException("SQL_CALC_FOUND_ROWS", ctx.dialect().name());
        }

        if (node.topSpec() != null && !ctx.dialect().paginationStyle().supportsTop()) {
            throw new UnsupportedOperationException("TOP is not supported by " + ctx.dialect().name());
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
     * Renders the DISTINCT clause when present.
     *
     * @param node query node.
     * @param ctx  render context.
     * @param w    sql writer.
     */
    protected void renderDistinctClause(SelectQuery node, RenderContext ctx, SqlWriter w) {
        if (node.distinct() != null) {
            w.space().append(node.distinct());
        }
    }

    /**
     * Hook for dialect-specific tokens rendered after DISTINCT and before
     * the projection list.
     *
     * @param node query node.
     * @param ctx  render context.
     * @param w    sql writer.
     */
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

    /**
     * Renders the projection list.
     *
     * @param node query node.
     * @param ctx  render context.
     * @param w    sql writer.
     */
    protected void renderSelectItems(SelectQuery node, RenderContext ctx, SqlWriter w) {
        w.space();
        w.comma(node.items());
    }

    /**
     * Renders the FROM clause.
     *
     * @param node query node.
     * @param ctx  render context.
     * @param w    sql writer.
     */
    protected void renderFromClause(SelectQuery node, RenderContext ctx, SqlWriter w) {
        if (node.from() != null) {
            w.newline().append("FROM").space();
            w.append(node.from());
        }
    }

    /**
     * Renders joins attached to the FROM clause.
     *
     * @param node query node.
     * @param ctx  render context.
     * @param w    sql writer.
     */
    protected void renderJoins(SelectQuery node, RenderContext ctx, SqlWriter w) {
        if (node.joins() != null) {
            for (var join : node.joins()) {
                w.newline();
                w.append(join);
            }
        }
    }

    /**
     * Renders the WHERE clause.
     *
     * @param node query node.
     * @param ctx  render context.
     * @param w    sql writer.
     */
    protected void renderWhereClause(SelectQuery node, RenderContext ctx, SqlWriter w) {
        if (node.where() != null) {
            w.newline().append("WHERE").space();
            w.append(node.where());
        }
    }

    /**
     * Renders the GROUP BY clause.
     *
     * @param node query node.
     * @param ctx  render context.
     * @param w    sql writer.
     */
    protected void renderGroupByClause(SelectQuery node, RenderContext ctx, SqlWriter w) {
        if (node.groupBy() != null) {
            w.newline().append(node.groupBy());
        }
    }

    /**
     * Renders the HAVING clause.
     *
     * @param node query node.
     * @param ctx  render context.
     * @param w    sql writer.
     */
    protected void renderHavingClause(SelectQuery node, RenderContext ctx, SqlWriter w) {
        if (node.having() != null) {
            w.newline().append("HAVING").space();
            w.append(node.having());
        }
    }

    /**
     * Renders the WINDOW clause.
     *
     * @param node query node.
     * @param ctx  render context.
     * @param w    sql writer.
     */
    protected void renderWindowClause(SelectQuery node, RenderContext ctx, SqlWriter w) {
        if (node.windows() != null) {
            for (var window : node.windows()) {
                w.newline().append(window);
            }
        }
    }

    /**
     * Renders the ORDER BY clause.
     *
     * @param node query node.
     * @param ctx  render context.
     * @param w    sql writer.
     */
    protected void renderOrderByClause(SelectQuery node, RenderContext ctx, SqlWriter w) {
        if (node.orderBy() != null) {
            w.newline().append(node.orderBy());
        }
    }

    /**
     * Renders the trailing pagination clause.
     *
     * @param node query node.
     * @param ctx  render context.
     * @param w    sql writer.
     */
    protected void renderPaginationClause(SelectQuery node, RenderContext ctx, SqlWriter w) {
        if (node.limitOffset() != null) {
            w.append(node.limitOffset());
        }
    }

    /**
     * Renders the locking clause.
     *
     * @param node query node.
     * @param ctx  render context.
     * @param w    sql writer.
     */
    protected void renderLockingClause(SelectQuery node, RenderContext ctx, SqlWriter w) {
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
