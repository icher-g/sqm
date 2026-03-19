package io.sqm.transpile;

import io.sqm.core.Statement;

import java.util.List;
import java.util.Objects;

/**
 * Result of a single transpilation rule execution.
 *
 * @param statement result statement after the rule
 * @param changed whether the statement changed
 * @param fidelity semantic fidelity of the rule outcome
 * @param warnings emitted non-blocking warnings
 * @param problems emitted blocking problems
 * @param description human-readable step description
 */
public record TranspileRuleResult(
    Statement statement,
    boolean changed,
    RewriteFidelity fidelity,
    List<TranspileWarning> warnings,
    List<TranspileProblem> problems,
    String description
) {
    /**
     * Creates a rule result.
     *
     * @param statement result statement after the rule
     * @param changed whether the statement changed
     * @param fidelity semantic fidelity of the rule outcome
     * @param warnings emitted non-blocking warnings
     * @param problems emitted blocking problems
     * @param description human-readable step description
     */
    public TranspileRuleResult {
        Objects.requireNonNull(statement, "statement");
        Objects.requireNonNull(fidelity, "fidelity");
        Objects.requireNonNull(warnings, "warnings");
        Objects.requireNonNull(problems, "problems");
        Objects.requireNonNull(description, "description");
        warnings = List.copyOf(warnings);
        problems = List.copyOf(problems);
    }

    /**
     * Creates an unchanged exact rule result.
     *
     * @param statement input statement
     * @param description human-readable step description
     * @return unchanged rule result
     */
    public static TranspileRuleResult unchanged(Statement statement, String description) {
        return new TranspileRuleResult(statement, false, RewriteFidelity.EXACT, List.of(), List.of(), description);
    }

    /**
     * Creates a rewritten rule result.
     *
     * @param statement result statement
     * @param fidelity rewrite fidelity
     * @param description human-readable step description
     * @return rewritten rule result
     */
    public static TranspileRuleResult rewritten(Statement statement, RewriteFidelity fidelity, String description) {
        return new TranspileRuleResult(statement, true, fidelity, List.of(), List.of(), description);
    }

    /**
     * Creates a rewritten rule result with a non-blocking warning.
     *
     * @param statement result statement
     * @param fidelity rewrite fidelity
     * @param code stable warning code
     * @param message human-readable warning description
     * @param description human-readable step description
     * @return rewritten warning rule result
     */
    public static TranspileRuleResult rewrittenWithWarning(
        Statement statement,
        RewriteFidelity fidelity,
        String code,
        String message,
        String description
    ) {
        return new TranspileRuleResult(
            statement,
            true,
            fidelity,
            List.of(new TranspileWarning(code, message)),
            List.of(),
            description
        );
    }

    /**
     * Creates an unsupported rule result.
     *
     * @param statement input statement
     * @param code stable diagnostic code
     * @param message human-readable problem description
     * @return unsupported rule result
     */
    public static TranspileRuleResult unsupported(Statement statement, String code, String message) {
        return new TranspileRuleResult(
            statement,
            false,
            RewriteFidelity.UNSUPPORTED,
            List.of(),
            List.of(new TranspileProblem(code, message, TranspileStage.REWRITE)),
            message
        );
    }
}
