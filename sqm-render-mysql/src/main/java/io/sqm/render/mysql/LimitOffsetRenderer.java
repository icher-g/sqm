package io.sqm.render.mysql;

import io.sqm.core.LimitOffset;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders MySQL LIMIT/OFFSET pagination fragments.
 */
public class LimitOffsetRenderer implements Renderer<LimitOffset> {

    /**
     * Creates a MySQL limit/offset renderer.
     */
    public LimitOffsetRenderer() {
    }

    /**
     * Gets the target type this renderer handles.
     *
     * @return target node type.
     */
    @Override
    public Class<LimitOffset> targetType() {
        return LimitOffset.class;
    }

    /**
     * Renders MySQL LIMIT/OFFSET clause.
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
            throw new UnsupportedOperationException("LIMIT ALL is not supported by MySQL");
        }

        if (node.limit() == null && node.offset() != null) {
            throw new UnsupportedOperationException("OFFSET without LIMIT is not supported by MySQL");
        }

        if (node.limit() != null) {
            w.newline().append("LIMIT").space().append(node.limit());
            if (node.offset() != null) {
                w.space().append("OFFSET").space().append(node.offset());
            }
        }
    }
}
