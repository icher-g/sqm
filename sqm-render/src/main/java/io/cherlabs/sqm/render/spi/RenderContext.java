package io.cherlabs.sqm.render.spi;

import io.cherlabs.sqm.render.PlaceholderResolver;

import java.util.Collection;
import java.util.stream.Collectors;

public interface RenderContext {
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
}
