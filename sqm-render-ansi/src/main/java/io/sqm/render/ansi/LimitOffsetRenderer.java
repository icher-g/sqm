package io.sqm.render.ansi;

import io.sqm.core.LimitOffset;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class LimitOffsetRenderer implements Renderer<LimitOffset> {
    @Override
    public Class<LimitOffset> targetType() {
        return LimitOffset.class;
    }

    @Override
    public void render(LimitOffset node, RenderContext ctx, SqlWriter w) {
        if (node.limit() == null && node.offset() == null) {
            return;
        }

        var limit = node.limit();
        var offset = node.offset();

        var ps = ctx.dialect().paginationStyle();
        if (ps.supportsLimitOffset()) {
            // Works for PostgreSQL, MySQL (LIMIT n [OFFSET m]); SQLite also accepts this order.
            if (limit != null) {
                w.newline().append("LIMIT").space().append(limit.toString());
                if (offset != null) {
                    w.space().append("OFFSET").space().append(offset.toString());
                }
            } else {
                // Only OFFSET without LIMIT (PG/SQLite accept this; MySQL ignores OFFSET without LIMIT)
                w.newline().append("OFFSET").space().append(offset.toString());
            }
            return;
        }

        if (ps.supportsOffsetFetch()) {
            long off = offset == null ? 0L : offset;
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
