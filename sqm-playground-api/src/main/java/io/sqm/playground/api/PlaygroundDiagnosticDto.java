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
 * @param clausePath optional semantic path of the affected clause
 * @param sourceDialect optional source dialect identifier for transpilation
 * @param targetDialect optional target dialect identifier for transpilation
 */
public record PlaygroundDiagnosticDto(
    DiagnosticSeverityDto severity,
    String code,
    String message,
    DiagnosticPhaseDto phase,
    Integer line,
    Integer column,
    Integer statementIndex,
    String clausePath,
    String sourceDialect,
    String targetDialect
) {

    /**
     * Creates a diagnostic with statement context and no transpilation metadata.
     * @param severity severity level
     * @param code stable code
     * @param message description
     * @param phase processing phase
     * @param line source line
     * @param column source column
     * @param statementIndex statement index
     */
    public PlaygroundDiagnosticDto(DiagnosticSeverityDto severity, String code, String message,
        DiagnosticPhaseDto phase, Integer line, Integer column, Integer statementIndex) {
        this(severity, code, message, phase, line, column, statementIndex, null, null, null);
    }

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
