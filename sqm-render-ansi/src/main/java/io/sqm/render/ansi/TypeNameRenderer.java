package io.sqm.render.ansi;

import io.sqm.core.TimeZoneSpec;
import io.sqm.core.TypeName;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

import java.util.Locale;
import java.util.Set;

public class TypeNameRenderer implements Renderer<TypeName> {

    private static final Set<String> validTimeTypes = Set.of("time", "timestamp");

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

        for (int i = 0; i < node.arrayDims(); i++) {
            w.append("[]");
        }

        if (node.timeZoneSpec() != TimeZoneSpec.NONE) {
            if (node.qualifiedName().isEmpty()) {
                throw new IllegalArgumentException("Timezone spec is only supported for qualified names.");
            }

            var name = node.qualifiedName().getLast().toLowerCase(Locale.ROOT);
            if (!validTimeTypes.contains(name)) {
                throw new IllegalArgumentException("Timezone spec is only supported for time types.");
            }

            switch (node.timeZoneSpec()) {
                case WITH_TIME_ZONE -> w.space().append("with time zone");
                case WITHOUT_TIME_ZONE -> w.space().append("without time zone");
                default -> throw new IllegalStateException("Unexpected value: " + node.timeZoneSpec());
            }
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
