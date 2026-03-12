package io.sqm.transpile;

/**
 * Non-blocking warning encountered during transpilation.
 *
 * @param code stable diagnostic code
 * @param message human-readable warning description
 */
public record TranspileWarning(String code, String message) {
}
