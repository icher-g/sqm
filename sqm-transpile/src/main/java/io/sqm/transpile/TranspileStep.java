package io.sqm.transpile;

/**
 * A single transpilation rule step.
 *
 * @param ruleId stable rewrite rule identifier
 * @param fidelity semantic fidelity of the rule outcome
 * @param description human-readable summary of the step
 * @param changed whether the rule changed the statement
 */
public record TranspileStep(
    String ruleId,
    RewriteFidelity fidelity,
    String description,
    boolean changed
) {
}
