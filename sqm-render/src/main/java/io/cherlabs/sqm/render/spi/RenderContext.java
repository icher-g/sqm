package io.cherlabs.sqm.render.spi;

import io.cherlabs.sqm.core.Entity;
import io.cherlabs.sqm.render.*;

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
     * Renders the entity into an {@link SqlWriter}.
     *
     * @param entity an entity to render.
     * @param w      a writer.
     */
    default <T extends Entity> void render(T entity, SqlWriter w) {
        w.append(entity);
    }

    /**
     * Renders the entity into an {@link SqlWriter}.
     *
     * @param entity an entity to render.
     */
    default <T extends Entity> SqlText render(T entity) {
        var w = new DefaultSqlWriter(this);
        w.append(entity);
        return w.toText(params().snapshot());
    }
}
