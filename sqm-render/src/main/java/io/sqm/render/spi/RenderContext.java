package io.sqm.render.spi;

import io.sqm.core.Node;
import io.sqm.render.*;

import java.util.Collection;
import java.util.stream.Collectors;

public interface RenderContext {

    static RenderContext of(SqlDialect dialect) {
        return new DefaultRenderContext(dialect);
    }

    SqlDialect dialect();

    ParamSink params();

    default ParameterizationMode parameterizationMode() {
        return ParameterizationMode.Inline;
    }

    default PlaceholderPreference placeholderPreference() {
        return PlaceholderPreference.Auto;
    }

    default String bindOrFormat(Object value) {
        if (parameterizationMode() == ParameterizationMode.Inline) {
            return dialect().formatter().format(value);
        } else {
            if (value instanceof Collection<?> col) {
                return col.stream()
                    .map(this::bindOrFormat)
                    .collect(Collectors.joining(", ", "(", ")"));
            } else {
                var token = PlaceholderResolver.next(this);
                params().add(value);
                return token;
            }
        }
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param w    a writer.
     */
    default <T extends Node> void render(T node, SqlWriter w) {
        w.append(node);
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     */
    default <T extends Node> SqlText render(T node) {
        var w = new DefaultSqlWriter(this);
        w.append(node);
        return w.toText(params().snapshot());
    }
}
