package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.core.views.Queries;
import io.cherlabs.sqm.render.spi.Renderer;
import io.cherlabs.sqm.render.SqlWriter;
import io.cherlabs.sqm.render.spi.RenderContext;

import java.util.Optional;

public class LimitOffsetRenderer implements Renderer<Query> {
    @Override
    public Class<Query> targetType() {
        return Query.class;
    }

    @Override
    public void render(Query entity, RenderContext ctx, SqlWriter w) {
        Optional<Long> limit = Queries.limit(entity);
        Optional<Long> offset = Queries.offset(entity);

        if (limit.isEmpty() && offset.isEmpty()) return;

        var ps = ctx.dialect().paginationStyle();
        if (ps.supportsLimitOffset()) {
            // Works for PostgreSQL, MySQL (LIMIT n [OFFSET m]); SQLite also accepts this order.
            if (limit.isPresent()) {
                w.newline().append("LIMIT").space().append(limit.get().toString());
                offset.ifPresent(l -> w.space().append("OFFSET").space().append(l.toString()));
            } else {
                // Only OFFSET without LIMIT (PG/SQLite accept this; MySQL ignores OFFSET without LIMIT)
                w.newline().append("OFFSET").space().append(offset.get().toString());
            }
            return;
        }

        if (ps.supportsOffsetFetch()) {
            long off = offset.orElse(0L);
            w.newline().append("OFFSET").space().append(Long.toString(off)).space().append("ROWS");
            limit.ifPresent(l -> w.space().append("FETCH NEXT").space().append(l.toString()).space().append("ROWS ONLY"));
            return;
        }

        if (ps.supportsTop()) {
            // We already injected TOP n in the SELECT head. OFFSET is not supported with TOP.
            if (offset.isPresent()) {
                throw new UnsupportedOperationException("Dialect supports TOP but not OFFSET; cannot render OFFSET.");
            }
            return; // nothing else to append
        }

        throw new UnsupportedOperationException("Pagination is not supported by the current dialect.");
    }
}
