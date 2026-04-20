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
 * @param statementIndex optional 1-based statement index for statement-sequence operations
 */
public record PlaygroundDiagnosticDto(
    DiagnosticSeverityDto severity,
    String code,
    String message,
    DiagnosticPhaseDto phase,
    Integer line,
    Integer column,
    Integer statementIndex
) {

    /**
     * Creates a diagnostic without statement-sequence context.
     *
     * @param severity severity level
     * @param code stable machine-readable code
     * @param message human-readable diagnostic message
     * @param phase processing phase that produced the diagnostic
     * @param line optional 1-based line number
     * @param column optional 1-based column number
     */
    public PlaygroundDiagnosticDto(
        DiagnosticSeverityDto severity,
        String code,
        String message,
        DiagnosticPhaseDto phase,
        Integer line,
        Integer column
    ) {
        this(severity, code, message, phase, line, column, null);
    }
}
