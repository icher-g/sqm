package io.sqm.render.ansi;

import io.sqm.core.LimitOffset;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders limit/offset pagination fragments according to dialect capabilities.
 */
public class LimitOffsetRenderer implements Renderer<LimitOffset> {
    /**
     * Creates a limit/offset renderer.
     */
    public LimitOffsetRenderer() {
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<LimitOffset> targetType() {
        return LimitOffset.class;
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(LimitOffset node, RenderContext ctx, SqlWriter w) {
        if (node.limit() == null && node.offset() == null && !node.limitAll()) {
            return;
        }

        var limit = node.limit();
        var offset = node.offset();
        var limitAll = node.limitAll();

        var ps = ctx.dialect().paginationStyle();
        if (ps.supportsLimitOffset()) {
            // Works for PostgreSQL, MySQL (LIMIT n [OFFSET m]); SQLite also accepts this order.
            if (limitAll) {
                w.newline().append("LIMIT ALL");
                if (offset != null) {
                    w.space().append("OFFSET").space().append(offset);
                }
            }
            else
                if (limit != null) {
                    w.newline().append("LIMIT").space().append(limit);
                    if (offset != null) {
                        w.space().append("OFFSET").space().append(offset);
                    }
                }
                else {
                    // Only OFFSET without LIMIT (PG/SQLite accept this; MySQL ignores OFFSET without LIMIT)
                    w.newline().append("OFFSET").space().append(offset);
                }
            return;
        }

        if (ps.supportsOffsetFetch()) {
            if (limitAll && offset == null) {
                return;
            }
            if (offset != null) {
                w.newline().append("OFFSET").space().append(offset).space().append("ROWS");
            }
            else {
                w.newline().append("OFFSET").space().append("0").space().append("ROWS");
            }
            if (limit != null) {
                w.space().append("FETCH NEXT").space().append(limit).space().append("ROWS ONLY");
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
