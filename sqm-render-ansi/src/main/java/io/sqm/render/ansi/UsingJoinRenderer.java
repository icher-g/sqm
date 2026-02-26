package io.sqm.render.ansi;

import io.sqm.core.UsingJoin;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders USING join clauses.
 */
public class UsingJoinRenderer implements Renderer<UsingJoin> {
    /**
     * Creates a USING-join renderer.
     */
    public UsingJoinRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(UsingJoin node, RenderContext ctx, SqlWriter w) {
        w.append("USING").space().append("(");
        for (int i = 0; i < node.usingColumns().size(); i++) {
            if (i > 0) {
                w.append(", ");
            }
            w.append(renderIdentifier(node.usingColumns().get(i), ctx.dialect().quoter()));
        }
        w.append(")");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<UsingJoin> targetType() {
        return UsingJoin.class;
    }
}
