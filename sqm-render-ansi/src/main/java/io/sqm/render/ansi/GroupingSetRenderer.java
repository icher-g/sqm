package io.sqm.render.ansi;

import io.sqm.core.GroupItem;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * ANSI renderer stub for grouping set items.
 */
public class GroupingSetRenderer implements Renderer<GroupItem.GroupingSet> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(GroupItem.GroupingSet node, RenderContext ctx, SqlWriter w) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.GROUPING_SETS)) {
            throw new UnsupportedDialectFeatureException("GROUPING SETS", ctx.dialect().name());
        }
        w.append("(").comma(node.items()).append(")");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends GroupItem.GroupingSet> targetType() {
        return GroupItem.GroupingSet.class;
    }
}
