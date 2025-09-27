package io.cherlabs.sqlmodel.render.spi;

import io.cherlabs.sqlmodel.render.PlaceholderResolver;

import java.util.Collection;
import java.util.stream.Collectors;

public interface RenderContext {
    SqlDialect dialect();

    ParamSink params();

    default ParameterizationMode parameterizationMode() {
        return ParameterizationMode.INLINE;
    }

    default PlaceholderPreference placeholderPreference() {
        return PlaceholderPreference.AUTO;
    }

    default String bindOrFormat(Object value) {
        if (parameterizationMode() == ParameterizationMode.INLINE) {
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
}
