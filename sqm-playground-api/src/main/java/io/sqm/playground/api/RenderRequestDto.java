package io.sqm.playground.api;

/**
 * Request payload for SQL rendering in the playground.
 *
 * @param sql input SQL text
 * @param sourceDialect source SQL dialect
 * @param targetDialect render target SQL dialect
 * @param parameterizationMode literal parameterization mode
 */
public record RenderRequestDto(
    String sql,
    SqlDialectDto sourceDialect,
    SqlDialectDto targetDialect,
    RenderParameterizationModeDto parameterizationMode
) {
    /**
     * Creates a render request using inline literal rendering.
     *
     * @param sql input SQL text
     * @param sourceDialect source SQL dialect
     * @param targetDialect render target SQL dialect
     */
    public RenderRequestDto(String sql, SqlDialectDto sourceDialect, SqlDialectDto targetDialect) {
        this(sql, sourceDialect, targetDialect, RenderParameterizationModeDto.inline);
    }

    /**
     * Normalizes optional request fields.
     */
    public RenderRequestDto {
        if (parameterizationMode == null) {
            parameterizationMode = RenderParameterizationModeDto.inline;
        }
    }
}
