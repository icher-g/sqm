package io.cherlabs.sqm.render;

import io.cherlabs.sqm.render.spi.ParamSink;
import io.cherlabs.sqm.render.spi.RenderContext;
import io.cherlabs.sqm.render.spi.SqlDialect;

/**
 * Represents a default implementation of {@link RenderContext}.
 * Uses {@link DefaultParamSink} as {@link ParamSink}.
 *
 * @param dialect an implementation of the {@link SqlDialect}.
 * @param params  an implementation of the {@link ParamSink}.
 */
public record DefaultRenderContext(SqlDialect dialect, ParamSink params) implements RenderContext {
    public DefaultRenderContext(SqlDialect dialect) {
        this(dialect, new DefaultParamSink());
    }
}
