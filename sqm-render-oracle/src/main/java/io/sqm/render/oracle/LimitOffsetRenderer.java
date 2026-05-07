package io.sqm.render.oracle;

import io.sqm.core.LimitOffset;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders Oracle row-limiting clauses.
 */
public class LimitOffsetRenderer implements Renderer<LimitOffset> {
    /**
     * Creates an Oracle limit/offset renderer.
     */
    public LimitOffsetRenderer() {
    }

    @Override
    public Class<LimitOffset> targetType() {
        return LimitOffset.class;
    }

    /**
     * Renders an Oracle row-limiting clause.
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
            throw new UnsupportedOperationException("LIMIT ALL is not supported by Oracle");
        }

        if (node.offset() != null) {
            w.newline().append("OFFSET").space().append(node.offset()).space().append("ROWS");
        }

        if (node.limit() != null) {
            if (node.offset() == null) {
                w.newline();
            }
            else {
                w.space();
            }
            w.append("FETCH FIRST").space().append(node.limit()).space().append("ROWS ONLY");
        }
    }
}
