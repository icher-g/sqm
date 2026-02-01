package io.sqm.render.ansi;

import io.sqm.core.GroupItem;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * ANSI renderer stub for {@code ROLLUP (...)}.
 */
public class RollupRenderer implements Renderer<GroupItem.Rollup> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(GroupItem.Rollup node, RenderContext ctx, SqlWriter w) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.ROLLUP)) {
            throw new UnsupportedDialectFeatureException("ROLLUP", ctx.dialect().name());
        }
        w.append("ROLLUP").space().append("(").comma(node.items()).append(")");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends GroupItem.Rollup> targetType() {
        return GroupItem.Rollup.class;
    }
}
