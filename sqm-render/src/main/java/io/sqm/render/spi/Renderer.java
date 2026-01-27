package io.sqm.render.spi;

import io.sqm.core.AliasedTableRef;
import io.sqm.core.Node;
import io.sqm.core.repos.Handler;
import io.sqm.render.SqlWriter;

/**
 * A base interface for all renderers.
 *
 * @param <T> the type of the node to render.
 */
public interface Renderer<T extends Node> extends Handler<T> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    void render(T node, RenderContext ctx, SqlWriter w);

    /**
     * Renders {@link AliasedTableRef} node that has an alias ana list of column aliases.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    default void renderAliased(AliasedTableRef node, RenderContext ctx, SqlWriter w) {
        if (node.alias() != null && !node.alias().isBlank()) {
            var quoter = ctx.dialect().quoter();
            w.space().append("AS").space().append(quoter.quoteIfNeeded(node.alias()));
            if (node.columnAliases() != null && !node.columnAliases().isEmpty()) {
                w.append("(");
                w.append(String.join(
                    ", ",
                    node.columnAliases().stream().map(a -> quoter.quoteIfNeeded(a)).toList()));
                w.append(")");
            }
        }
    }
}
