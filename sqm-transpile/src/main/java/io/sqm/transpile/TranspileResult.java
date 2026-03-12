package io.sqm.transpile;

import io.sqm.core.Statement;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of a transpilation attempt.
 *
 * @param status overall transpilation status
 * @param sourceAst parsed source AST when available
 * @param transpiledAst final AST after rewrite when available
 * @param sql rendered target SQL when available
 * @param steps transpilation rule steps that were executed
 * @param problems blocking problems encountered during transpilation
 * @param warnings non-blocking warnings encountered during transpilation
 */
public record TranspileResult(
    TranspileStatus status,
    Optional<Statement> sourceAst,
    Optional<Statement> transpiledAst,
    Optional<String> sql,
    List<TranspileStep> steps,
    List<TranspileProblem> problems,
    List<TranspileWarning> warnings
) {
    /**
     * Creates a transpilation result.
     *
     * @param status overall transpilation status
     * @param sourceAst parsed source AST when available
     * @param transpiledAst final AST after rewrite when available
     * @param sql rendered target SQL when available
     * @param steps transpilation rule steps that were executed
     * @param problems blocking problems encountered during transpilation
     * @param warnings non-blocking warnings encountered during transpilation
     */
    public TranspileResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(sourceAst, "sourceAst");
        Objects.requireNonNull(transpiledAst, "transpiledAst");
        Objects.requireNonNull(sql, "sql");
        Objects.requireNonNull(steps, "steps");
        Objects.requireNonNull(problems, "problems");
        Objects.requireNonNull(warnings, "warnings");
        steps = List.copyOf(steps);
        problems = List.copyOf(problems);
        warnings = List.copyOf(warnings);
    }

    /**
     * Returns whether transpilation completed successfully.
     *
     * @return {@code true} when the result is successful
     */
    public boolean success() {
        return status == TranspileStatus.SUCCESS || status == TranspileStatus.SUCCESS_WITH_WARNINGS;
    }
}
