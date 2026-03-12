package io.sqm.transpile;

/**
 * Overall result of a transpilation request.
 */
public enum TranspileStatus {
    /**
     * Parsing the source SQL failed.
     */
    PARSE_FAILED,
    /**
     * Transpilation was blocked by an unsupported construct or policy.
     */
    UNSUPPORTED,
    /**
     * Target validation failed.
     */
    VALIDATION_FAILED,
    /**
     * Rendering the target SQL failed.
     */
    RENDER_FAILED,
    /**
     * Transpilation completed successfully.
     */
    SUCCESS,
    /**
     * Transpilation completed successfully with non-blocking warnings.
     */
    SUCCESS_WITH_WARNINGS
}
