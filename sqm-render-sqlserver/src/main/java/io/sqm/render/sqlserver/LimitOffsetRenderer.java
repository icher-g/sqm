package io.sqm.render.sqlserver;

import io.sqm.core.LimitOffset;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders SQL Server OFFSET/FETCH pagination fragments.
 */
public class LimitOffsetRenderer implements Renderer<LimitOffset> {

    /**
     * Creates a SQL Server limit/offset renderer.
     */
    public LimitOffsetRenderer() {
    }

    @Override
    public Class<LimitOffset> targetType() {
        return LimitOffset.class;
    }

    /**
     * Renders SQL Server OFFSET/FETCH clause.
     *
     * @param node limit/offset node.
     * @param ctx  render context.
     * @param w    SQL writer.
     */
    @Override
    public void render(LimitOffset node, RenderContext ctx, SqlWriter w) {
        if (node.limit() == null && node.offset() == null && !node.limitAll()) {
            return;
        }

        if (node.limitAll()) {
            throw new UnsupportedOperationException("LIMIT ALL is not supported by SQL Server");
        }

        if (node.offset() == null && node.limit() != null) {
            return; // TOP is rendered by the SQL Server SELECT renderer.
        }

        if (node.offset() == null) {
            throw new UnsupportedOperationException("FETCH requires OFFSET in SQL Server");
        }

        w.newline().append("OFFSET").space().append(node.offset()).space().append("ROWS");
        if (node.limit() != null) {
            w.space().append("FETCH NEXT").space().append(node.limit()).space().append("ROWS ONLY");
        }
    }
}
