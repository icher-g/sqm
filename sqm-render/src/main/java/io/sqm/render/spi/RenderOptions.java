package io.sqm.render.spi;

/**
 * Options controlling how a SQL query should be rendered, in particular
 * how parameters are represented in the final SQL string.
 *
 * <p>
 * Typical usage:
 * <pre>{@code
 * var opts = RenderOptions.of(ParameterizationMode.POSITIONAL);
 * var sql  = ctx.render(query, opts).sql();
 * }</pre>
 *
 * @param parameterizationMode the strategy for representing parameters during rendering
 */
public record RenderOptions(ParameterizationMode parameterizationMode) {
    /**
     * Creates a {@code RenderOptions} instance for the given parameterization mode.
     *
     * @param parameterizationMode the desired parameterization strategy.
     * @return a new {@code RenderOptions}.
     */
    public static RenderOptions of(ParameterizationMode parameterizationMode) {
        return new RenderOptions(parameterizationMode);
    }
}

