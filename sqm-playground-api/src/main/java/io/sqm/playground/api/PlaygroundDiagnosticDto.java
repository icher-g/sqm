package io.sqm.playground.api;

/**
 * Transport-neutral diagnostic emitted by playground operations.
 *
 * @param severity severity level
 * @param code stable machine-readable code
 * @param message human-readable diagnostic message
 * @param phase processing phase that produced the diagnostic
 * @param line optional 1-based line number
 * @param column optional 1-based column number
 */
public record PlaygroundDiagnosticDto(
    DiagnosticSeverityDto severity,
    String code,
    String message,
    DiagnosticPhaseDto phase,
    Integer line,
    Integer column
) {
}
