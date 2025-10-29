package io.sqm.render.ansi;

import io.sqm.core.CompositeQuery;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class CompositeQueryRenderer implements Renderer<CompositeQuery> {

    private final SetOperatorRenderer operatorRenderer = new SetOperatorRenderer();
    private final LimitOffsetRenderer limitOffsetRenderer = new LimitOffsetRenderer();

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(CompositeQuery node, RenderContext ctx, SqlWriter w) {
        var terms = node.terms();
        var ops = node.ops();

        // Render terms with safe parentheses
        for (int i = 0; i < terms.size(); i++) {
            var term = terms.get(i);
            w.append(term, terms.size() > 1, true);

            if (i < ops.size()) {
                w.newline();
                operatorRenderer.render(ops.get(i), ctx, w);
                w.newline();
            }
        }

        // Final ORDER BY (applies to the whole composite)
        if (node.orderBy() != null) {
            w.newline().append("ORDER BY").space();
            w.append(node.orderBy());
        }

        // Final pagination (ANSI OFFSET/FETCH)
        // Pagination tail — pick the right style
        if (node.limitOffset() != null) {
            limitOffsetRenderer.render(node.limitOffset(), ctx, w);
        }
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<CompositeQuery> targetType() {
        return CompositeQuery.class;
    }
}
