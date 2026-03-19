package io.sqm.render.mysql;

import io.sqm.core.GroupBy;
import io.sqm.core.GroupItem;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders MySQL {@code GROUP BY} clauses, canonicalizing
 * {@code ROLLUP(...)} to {@code ... WITH ROLLUP} when possible.
 */
public class GroupByRenderer implements Renderer<GroupBy> {

    /**
     * Creates a MySQL GROUP BY renderer.
     */
    public GroupByRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(GroupBy node, RenderContext ctx, SqlWriter w) {
        w.append("GROUP BY").space();

        if (node.items().size() == 1 && node.items().getFirst() instanceof GroupItem.Rollup rollup) {
            if (!ctx.dialect().capabilities().supports(SqlFeature.ROLLUP)) {
                throw new UnsupportedDialectFeatureException("ROLLUP", ctx.dialect().name());
            }
            w.comma(rollup.items());
            w.space().append("WITH ROLLUP");
            return;
        }

        w.comma(node.items());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<GroupBy> targetType() {
        return GroupBy.class;
    }
}
