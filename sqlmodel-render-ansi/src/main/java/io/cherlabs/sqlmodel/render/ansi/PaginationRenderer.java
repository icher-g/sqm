package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.Query;
import io.cherlabs.sqlmodel.render.Renderer;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.spi.RenderContext;

public class PaginationRenderer implements Renderer<Query<?>> {
    @Override
    public Class<Query<?>> targetType() {
        return null;
    }

    @Override
    public void render(Query<?> entity, RenderContext ctx, SqlWriter w) {
        Long limit = entity.limit();
        Long offset = entity.offset();

        if (limit == null && offset == null) return;

        var ps = ctx.dialect().paginationStyle();
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
