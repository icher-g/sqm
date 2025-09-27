package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.Query;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

import java.util.List;

public final class QueryRenderer implements Renderer<Query> {

    private static boolean has(List<?> parts) {
        return parts != null && !parts.isEmpty();
    }

    @Override
    public void render(Query q, RenderContext ctx, SqlWriter w) {
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
        Long limit = q.limit();
        Long offset = q.offset();

        if (limit == null && offset == null) return;

        if (ps.supportsLimitOffset()) {
            // Works for PostgreSQL, MySQL (LIMIT n [OFFSET m]); SQLite also accepts this order.
            if (limit != null) {
                w.newline().append("LIMIT").space().append(limit.toString());
                if (offset != null) w.space().append("OFFSET").space().append(offset.toString());
            } else {
                // Only OFFSET without LIMIT (PG/SQLite accept this; MySQL ignores OFFSET without LIMIT)
                w.newline().append("OFFSET").space().append(offset.toString());
            }
            return;
        }

        if (ps.supportsOffsetFetch()) {
            // SQL Server 2012+, Oracle 12c+, DB2, etc. require ORDER BY to use OFFSET/FETCH
            if (!has(q.orderBy())) {
                throw new IllegalStateException("This dialect requires ORDER BY when using OFFSET/FETCH pagination.");
            }
            long off = offset != null ? offset : 0L;
            w.newline().append("OFFSET").space().append(Long.toString(off)).space().append("ROWS");
            if (limit != null) {
                w.space().append("FETCH NEXT").space().append(limit.toString()).space().append("ROWS ONLY");
            }
            return;
        }

        if (ps.supportsTop()) {
            // We already injected TOP n in the SELECT head. OFFSET is not supported with TOP.
            if (offset != null) {
                throw new UnsupportedOperationException("Dialect supports TOP but not OFFSET; cannot render OFFSET.");
            }
            return; // nothing else to append
        }

        throw new UnsupportedOperationException("Pagination is not supported by the current dialect.");
    }
}
