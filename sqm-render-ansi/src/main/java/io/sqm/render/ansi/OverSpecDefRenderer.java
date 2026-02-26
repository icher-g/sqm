package io.sqm.render.ansi;

import io.sqm.core.OverSpec;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders inline window OVER definitions.
 */
public class OverSpecDefRenderer implements Renderer<OverSpec.Def> {
    /**
     * Creates an OVER-definition renderer.
     */
    public OverSpecDefRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(OverSpec.Def node, RenderContext ctx, SqlWriter w) {
        boolean addSpace = false;
        if (node.baseWindow() != null) {
            w.append(renderIdentifier(node.baseWindow(), ctx.dialect().quoter()));
            addSpace = true;
        }
        if (node.partitionBy() != null && !node.partitionBy().items().isEmpty()) {
            if (addSpace) w.space();
            w.append(node.partitionBy());
            addSpace = true;
        }
        if (node.orderBy() != null && !node.orderBy().items().isEmpty()) {
            if (addSpace) w.space();
            w.append(node.orderBy());
            addSpace = true;
        }
        if (node.frame() != null) {
            if (addSpace) w.space();
            w.append(node.frame());
            addSpace = true;
        }
        if (node.exclude() != null) {
            if (addSpace) w.space();
            w.append("EXCLUDE").space();
            w.append(excludeToString(node.exclude()));
        }
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<OverSpec.Def> targetType() {
        return OverSpec.Def.class;
    }

    private String excludeToString(OverSpec.Exclude exclude) {
        switch (exclude) {
            case CURRENT_ROW -> {
                return "CURRENT ROW";
            }
            case GROUP -> {
                return "GROUP";
            }
            case TIES -> {
                return "TIES";
            }
            case NO_OTHERS -> {
                return "NO OTHERS";
            }
            default -> throw new IllegalStateException("Unexpected value: " + exclude);
        }
    }
}
