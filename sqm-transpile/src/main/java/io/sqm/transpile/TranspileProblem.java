package io.sqm.transpile;

import io.sqm.core.dialect.SqlDialectId;
import java.util.Objects;

/**
 * Blocking problem encountered during transpilation.
 *
 * @param code stable diagnostic code
 * @param message human-readable problem description
 * @param stage pipeline stage where the problem occurred
 * @param sourceOffset zero-based source character offset when the problem can be mapped to the input SQL
 * @param line one-based source line when the problem can be mapped to the input SQL
 * @param column one-based source column when the problem can be mapped to the input SQL
 * @param statementIndex optional one-based statement index when transpiling a statement sequence
 * @param clausePath optional stable semantic path identifying the affected clause or relation
 * @param sourceDialect source dialect when supplied by a transpiler
 * @param targetDialect target dialect when supplied by a transpiler
 */
public record TranspileProblem(
    String code,
    String message,
    TranspileStage stage,
    Integer sourceOffset,
    Integer line,
    Integer column,
    Integer statementIndex,
    String clausePath,
    SqlDialectId sourceDialect,
    SqlDialectId targetDialect
) {
    /**
     * Creates a legacy path-aware problem without dialect metadata.
     *
     * @param code diagnostic code
     * @param message description
     * @param stage pipeline stage
     * @param sourceOffset source offset
     * @param line source line
     * @param column source column
     * @param statementIndex statement index
     * @param clausePath stable node path
     */
    public TranspileProblem(String code, String message, TranspileStage stage, Integer sourceOffset,
        Integer line, Integer column, Integer statementIndex, String clausePath) {
        this(code, message, stage, sourceOffset, line, column, statementIndex, clausePath, null, null);
    }

    /**
     * Returns a copy carrying the exact source and target dialect identifiers.
     *
     * @param source source dialect
     * @param target target dialect
     * @return annotated problem
     */
    public TranspileProblem withDialects(SqlDialectId source, SqlDialectId target) {
        return new TranspileProblem(code, message, stage, sourceOffset, line, column, statementIndex,
            clausePath, Objects.requireNonNull(source, "source"), Objects.requireNonNull(target, "target"));
    }
    /**
     * Creates a transpile problem using the legacy coordinate and statement-index shape.
     *
     * @param code stable diagnostic code
     * @param message human-readable problem description
     * @param stage pipeline stage where the problem occurred
     * @param sourceOffset zero-based source character offset when available
     * @param line one-based source line when available
     * @param column one-based source column when available
     * @param statementIndex optional one-based statement index
     */
    public TranspileProblem(
        String code,
        String message,
        TranspileStage stage,
        Integer sourceOffset,
        Integer line,
        Integer column,
        Integer statementIndex
    ) {
        this(code, message, stage, sourceOffset, line, column, statementIndex, null);
    }

    /**
     * Creates a transpile problem without source coordinates.
     *
     * @param code stable diagnostic code
     * @param message human-readable problem description
     * @param stage pipeline stage where the problem occurred
     */
    public TranspileProblem(String code, String message, TranspileStage stage) {
        this(code, message, stage, null, null, null, null, null);
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
        this(code, message, stage, sourceOffset, null, null, null, null);
    }

    /**
     * Creates a transpile problem with source coordinates.
     *
     * @param code stable diagnostic code
     * @param message human-readable problem description
     * @param stage pipeline stage where the problem occurred
     * @param sourceOffset zero-based source character offset when available
     * @param line one-based source line when available
     * @param column one-based source column when available
     */
    public TranspileProblem(String code, String message, TranspileStage stage, Integer sourceOffset, Integer line, Integer column) {
        this(code, message, stage, sourceOffset, line, column, null, null);
    }

    /**
     * Returns a copy of this problem annotated with a one-based statement index.
     *
     * @param statementIndex one-based statement index
     * @return problem with statement index
     */
    public TranspileProblem withStatementIndex(int statementIndex) {
        if (statementIndex < 1) {
            throw new IllegalArgumentException("statementIndex must be greater than zero");
        }
        return new TranspileProblem(code, message, stage, sourceOffset, line, column, statementIndex, clausePath,
            sourceDialect, targetDialect);
    }
}
