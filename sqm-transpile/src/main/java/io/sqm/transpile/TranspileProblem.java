package io.sqm.transpile;

/**
 * Blocking problem encountered during transpilation.
 *
 * @param code stable diagnostic code
 * @param message human-readable problem description
 * @param stage pipeline stage where the problem occurred
 * @param sourceOffset zero-based source character offset when the problem can be mapped to the input SQL
 * @param line one-based source line when the problem can be mapped to the input SQL
 * @param column one-based source column when the problem can be mapped to the input SQL
 */
public record TranspileProblem(
    String code,
    String message,
    TranspileStage stage,
    Integer sourceOffset,
    Integer line,
    Integer column
) {
    /**
     * Creates a transpile problem without source coordinates.
     *
     * @param code stable diagnostic code
     * @param message human-readable problem description
     * @param stage pipeline stage where the problem occurred
     */
    public TranspileProblem(String code, String message, TranspileStage stage) {
        this(code, message, stage, null, null, null);
    }

    /**
     * Creates a transpile problem with a source offset but without explicit line and column.
     *
     * @param code stable diagnostic code
     * @param message human-readable problem description
     * @param stage pipeline stage where the problem occurred
     * @param sourceOffset zero-based source character offset when available
     */
    public TranspileProblem(String code, String message, TranspileStage stage, Integer sourceOffset) {
        this(code, message, stage, sourceOffset, null, null);
    }
}
