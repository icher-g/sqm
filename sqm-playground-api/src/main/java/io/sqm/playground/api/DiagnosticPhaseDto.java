package io.sqm.playground.api;

/**
 * Identifies the processing phase that produced a playground diagnostic.
 */
public enum DiagnosticPhaseDto {
    http,
    parse,
    render,
    validate,
    transpile;

    /**
     * Resolves a phase from its lowercase API value.
     *
     * @param value lowercase API value
     * @return matching phase
     */
    public static DiagnosticPhaseDto fromValue(String value) {
        try {
            return DiagnosticPhaseDto.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown diagnostic phase: " + value, e);
        }
    }
}
