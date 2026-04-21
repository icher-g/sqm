package io.sqm.transpile;

/**
 * Non-blocking warning encountered during transpilation.
 *
 * @param code stable diagnostic code
 * @param message human-readable warning description
 * @param statementIndex optional one-based statement index when transpiling a statement sequence
 */
public record TranspileWarning(String code, String message, Integer statementIndex) {
    /**
     * Creates a transpile warning without a statement index.
     *
     * @param code stable diagnostic code
     * @param message human-readable warning description
     */
    public TranspileWarning(String code, String message) {
        this(code, message, null);
    }

    /**
     * Returns a copy of this warning annotated with a one-based statement index.
     *
     * @param statementIndex one-based statement index
     * @return warning with statement index
     */
    public TranspileWarning withStatementIndex(int statementIndex) {
        if (statementIndex < 1) {
            throw new IllegalArgumentException("statementIndex must be greater than zero");
        }
        return new TranspileWarning(code, message, statementIndex);
    }
}
