package io.sqm.render.ansi;

import io.sqm.core.TimeZoneSpec;
import io.sqm.core.TypeName;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class TypeNameRenderer implements Renderer<TypeName> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(TypeName node, RenderContext ctx, SqlWriter w) {
        if (node.keyword().isPresent())
            w.append(node.keyword().get().sql());
        else
            // write the parts: pg_catalog.int4 or double precision
            w.append(String.join(".", node.qualifiedName()));

        if (!node.modifiers().isEmpty()) {
            // add modifiers: numeric(10,2)
            w.append("(");
            w.comma(node.modifiers());
            w.append(")");
        }

        if (node.arrayDims() > 0) {
            throw new UnsupportedOperationException("ANSI renderer does not support array type syntax []");
        }

        if (node.timeZoneSpec() != TimeZoneSpec.NONE) {
            throw new UnsupportedOperationException("ANSI renderer does not support WITH/WITHOUT TIME ZONE type clauses");
        }
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends TypeName> targetType() {
        return TypeName.class;
    }
}
