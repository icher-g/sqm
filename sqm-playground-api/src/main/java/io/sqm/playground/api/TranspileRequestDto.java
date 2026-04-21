package io.sqm.playground.api;

/**
 * Request payload for SQL transpilation in the playground.
 *
 * @param sql input SQL text
 * @param sourceDialect source SQL dialect
 * @param targetDialect target SQL dialect
 * @param parameterizationMode literal parameterization mode used for rendered output
 */
public record TranspileRequestDto(
    String sql,
    SqlDialectDto sourceDialect,
    SqlDialectDto targetDialect,
    RenderParameterizationModeDto parameterizationMode
) {
    /**
     * Creates a transpile request using inline literal rendering.
     *
     * @param sql input SQL text
     * @param sourceDialect source SQL dialect
     * @param targetDialect target SQL dialect
     */
    public TranspileRequestDto(String sql, SqlDialectDto sourceDialect, SqlDialectDto targetDialect) {
        this(sql, sourceDialect, targetDialect, RenderParameterizationModeDto.inline);
    }

    /**
     * Normalizes optional request fields.
     */
    public TranspileRequestDto {
        if (parameterizationMode == null) {
            parameterizationMode = RenderParameterizationModeDto.inline;
        }
    }
}
