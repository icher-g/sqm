package io.sqm.render.ansi.query;

import io.sqm.core.SelectQuery;
import io.sqm.render.SqlWriter;
import io.sqm.render.ansi.LimitOffsetRenderer;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

import java.util.List;

public final class SelectQueryRenderer implements Renderer<SelectQuery> {

    private static boolean has(List<?> parts) {
        return parts != null && !parts.isEmpty();
    }

    @Override
    public Class<SelectQuery> targetType() {
        return SelectQuery.class;
    }

    @Override
    public void render(SelectQuery q, RenderContext ctx, SqlWriter w) {
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
        w.comma(q.columns()); // each column rendered via its own registered renderer

        // FROM
        if (q.table() != null) {
            w.newline().append("FROM").space();
            w.append(q.table()); // table (with optional schema/alias) rendered by its renderer
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
        if (q.groupBy() != null) {
            w.newline().append("GROUP BY").space();
            w.append(q.groupBy());
        }

        // HAVING
        if (q.having() != null) {
            w.newline().append("HAVING").space();
            w.append(q.having());
        }

        // ORDER BY
        if (q.orderBy() != null) {
            w.newline().append("ORDER BY").space();
            w.append(q.orderBy());
        }

        // Pagination tail — pick the right style
        new LimitOffsetRenderer().render(q, ctx, w);
    }
}
