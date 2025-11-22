package io.sqm.render;

import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.SqlDialect;

/**
 * Represents a default implementation of {@link RenderContext}.
 *
 * @param dialect an implementation of the {@link SqlDialect}.
 */
public record DefaultRenderContext(SqlDialect dialect) implements RenderContext {
}
