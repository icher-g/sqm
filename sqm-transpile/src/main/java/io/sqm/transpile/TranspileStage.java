package io.sqm.transpile;

/**
 * Pipeline stage used for transpilation diagnostics.
 */
public enum TranspileStage {
    /**
     * Source SQL parsing.
     */
    PARSE,
    /**
     * Source normalization before dialect-specific rewrites.
     */
    NORMALIZE,
    /**
     * Dialect-aware rewrite execution.
     */
    REWRITE,
    /**
     * Target validation against schema and dialect constraints.
     */
    VALIDATE,
    /**
     * Target SQL rendering.
     */
    RENDER
}
