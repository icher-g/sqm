package io.sqm.playground.api;

/**
 * Identifies the processing phase that produced a playground diagnostic.
 */
public enum DiagnosticPhaseDto {
    /**
     * an http issue
     */
    http,
    /**
     * a parse issue
     */
    parse,
    /**
     * a DSL generation issue
     */
    dsl,
    /**
     * a renderer issue
     */
    render,
    /**
     * a validation issue
     */
    validate,
    /**
     * a transpilation issue
     */
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
