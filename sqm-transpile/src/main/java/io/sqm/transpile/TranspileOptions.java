package io.sqm.transpile;

/**
 * Options controlling transpilation behavior.
 *
 * @param allowApproximateRewrites whether approximate rewrites may be applied
 * @param failOnWarnings whether warnings should fail the transpilation request
 * @param validateTarget whether target validation should run when a target schema is available
 * @param renderSql whether target SQL rendering should be attempted on success
 */
public record TranspileOptions(
    boolean allowApproximateRewrites,
    boolean failOnWarnings,
    boolean validateTarget,
    boolean renderSql
) {
    /**
     * Returns default transpilation options.
     *
     * @return default options
     */
    public static TranspileOptions defaults() {
        return new TranspileOptions(false, false, true, true);
    }
}
