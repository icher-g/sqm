package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.Query;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

import java.util.List;

public final class QueryRenderer implements Renderer<Query<?>> {

    private static boolean has(List<?> parts) {
        return parts != null && !parts.isEmpty();
    }

    @Override
    public void render(Query<?> q, RenderContext ctx, SqlWriter w) {
        // SELECT
        w.append("SELECT");
        if (q.distinct() != null && q.distinct()) {
            w.space().append("DISTINCT");
        }

        var ps = ctx.dialect().paginationStyle();

        // Inject TOP n into the head if dialect supports TOP and there's no OFFSET
        if (ps.supportsTop() && q.limit() != null && q.offset() == null) {
            w.space().append("TOP ").append(q.limit().toString());
        }

        w.space();
        w.comma(q.select()); // each column rendered via its own registered renderer

        // FROM
        if (q.from() != null) {
            w.newline().append("FROM").space();
            w.append(q.from()); // table (with optional schema/alias) rendered by its renderer
        }

        // JOINS
        if (has(q.joins())) {
            for (var j : q.joins()) {
                w.newline();
                w.append(j); // each join rendered by AnsiTableJoinRenderer (or dialect-specific)
            }
        }

        // WHERE
        if (q.where() != null) {
            w.newline().append("WHERE").space();
            w.append(q.where()); // delegate to filter renderer (composite/single/etc.)
        }

        // GROUP BY
        if (has(q.groupBy())) {
            w.newline().append("GROUP BY").space();
            w.comma(q.groupBy()); // columns/expressions
        }

        // HAVING
        if (q.having() != null) {
            w.newline().append("HAVING").space();
            w.append(q.having());
        }

        // ORDER BY
        if (has(q.orderBy())) {
            w.newline().append("ORDER BY").space();
            w.comma(q.orderBy()); // order items with direction/nulls etc. via item renderer
        }

        // Pagination tail — pick the right style
        new PaginationRenderer().render(q, ctx, w);
    }
}
