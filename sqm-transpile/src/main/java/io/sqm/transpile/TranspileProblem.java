package io.sqm.transpile;

/**
 * Blocking problem encountered during transpilation.
 *
 * @param code stable diagnostic code
 * @param message human-readable problem description
 * @param stage pipeline stage where the problem occurred
 */
public record TranspileProblem(String code, String message, TranspileStage stage) {
}
