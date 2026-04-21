package io.sqm.transpile;

import io.sqm.render.spi.ParameterizationMode;

/**
 * Options controlling transpilation behavior.
 *
 * @param allowApproximateRewrites whether approximate rewrites may be applied
 * @param failOnWarnings whether warnings should fail the transpilation request
 * @param validateTarget whether target validation should run when a target schema is available
 * @param renderSql whether target SQL rendering should be attempted on success
 * @param renderParameterizationMode parameterization mode used when rendering target SQL
 */
public record TranspileOptions(
    boolean allowApproximateRewrites,
    boolean failOnWarnings,
    boolean validateTarget,
    boolean renderSql,
    ParameterizationMode renderParameterizationMode
) {
    /**
     * Creates transpilation options using inline SQL rendering.
     *
     * @param allowApproximateRewrites whether approximate rewrites may be applied
     * @param failOnWarnings whether warnings should fail the transpilation request
     * @param validateTarget whether target validation should run when a target schema is available
     * @param renderSql whether target SQL rendering should be attempted on success
     */
    public TranspileOptions(
        boolean allowApproximateRewrites,
        boolean failOnWarnings,
        boolean validateTarget,
        boolean renderSql
    ) {
        this(allowApproximateRewrites, failOnWarnings, validateTarget, renderSql, ParameterizationMode.Inline);
    }

    /**
     * Normalizes optional transpilation options.
     */
    public TranspileOptions {
        if (renderParameterizationMode == null) {
            renderParameterizationMode = ParameterizationMode.Inline;
        }
    }

    /**
     * Returns default transpilation options.
     *
     * @return default options
     */
    public static TranspileOptions defaults() {
        return new TranspileOptions(false, false, true, true, ParameterizationMode.Inline);
    }
}
