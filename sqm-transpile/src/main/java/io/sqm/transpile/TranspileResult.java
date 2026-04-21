package io.sqm.transpile;

import io.sqm.core.Node;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of a transpilation attempt.
 *
 * @param status overall transpilation status
 * @param sourceAst parsed source AST or statement sequence when available
 * @param transpiledAst final AST or statement sequence after rewrite when available
 * @param sql rendered target SQL when available
 * @param params rendered SQL bind parameters in order
 * @param steps transpilation rule steps that were executed
 * @param problems blocking problems encountered during transpilation
 * @param warnings non-blocking warnings encountered during transpilation
 */
public record TranspileResult(
    TranspileStatus status,
    Optional<Node> sourceAst,
    Optional<Node> transpiledAst,
    Optional<String> sql,
    List<Object> params,
    List<TranspileStep> steps,
    List<TranspileProblem> problems,
    List<TranspileWarning> warnings
) {
    /**
     * Creates a transpilation result.
     *
     * @param status overall transpilation status
     * @param sourceAst parsed source AST or statement sequence when available
     * @param transpiledAst final AST or statement sequence after rewrite when available
     * @param sql rendered target SQL when available
     * @param params rendered SQL bind parameters in order
     * @param steps transpilation rule steps that were executed
     * @param problems blocking problems encountered during transpilation
     * @param warnings non-blocking warnings encountered during transpilation
     */
    public TranspileResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(sourceAst, "sourceAst");
        Objects.requireNonNull(transpiledAst, "transpiledAst");
        Objects.requireNonNull(sql, "sql");
        Objects.requireNonNull(params, "params");
        Objects.requireNonNull(steps, "steps");
        Objects.requireNonNull(problems, "problems");
        Objects.requireNonNull(warnings, "warnings");
        params = List.copyOf(params);
        steps = List.copyOf(steps);
        problems = List.copyOf(problems);
        warnings = List.copyOf(warnings);
    }

    /**
     * Creates a transpilation result without rendered bind parameters.
     *
     * @param status overall transpilation status
     * @param sourceAst parsed source AST or statement sequence when available
     * @param transpiledAst final AST or statement sequence after rewrite when available
     * @param sql rendered target SQL when available
     * @param steps transpilation rule steps that were executed
     * @param problems blocking problems encountered during transpilation
     * @param warnings non-blocking warnings encountered during transpilation
     */
    public TranspileResult(
        TranspileStatus status,
        Node sourceAst,
        Node transpiledAst,
        String sql,
        List<TranspileStep> steps,
        List<TranspileProblem> problems,
        List<TranspileWarning> warnings
    ) {
        this(status, Optional.ofNullable(sourceAst), Optional.ofNullable(transpiledAst), Optional.ofNullable(sql), List.of(), steps, problems, warnings);
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
