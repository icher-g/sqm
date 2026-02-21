package io.sqm.control;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Audit event emitted for each middleware decision.
 *
 * @param rawSql        original input SQL
 * @param normalizedSql normalized SQL form used for audit grouping
 * @param appliedRules  applied reason codes during evaluation
 * @param rewrittenSql  rewritten SQL when decision is {@link DecisionKind#REWRITE}
 * @param decision      final decision payload
 * @param context       execution context used for evaluation
 * @param durationNanos end-to-end evaluation latency in nanoseconds
 */
public record AuditEvent(
    String rawSql,
    String normalizedSql,
    List<ReasonCode> appliedRules,
    String rewrittenSql,
    DecisionResult decision,
    ExecutionContext context,
    long durationNanos
) implements Serializable {
    /**
     * Validates audit payload invariants.
     *
     * @param rawSql        original input SQL
     * @param normalizedSql normalized SQL
     * @param appliedRules  applied reason codes
     * @param rewrittenSql  rewritten SQL
     * @param decision      final decision
     * @param context       execution context
     * @param durationNanos evaluation duration in nanoseconds
     */
    public AuditEvent {
        if (rawSql == null || rawSql.isBlank()) {
            throw new IllegalArgumentException("rawSql must not be blank");
        }
        if (normalizedSql == null || normalizedSql.isBlank()) {
            throw new IllegalArgumentException("normalizedSql must not be blank");
        }
        Objects.requireNonNull(appliedRules, "appliedRules must not be null");
        Objects.requireNonNull(decision, "decision must not be null");
        Objects.requireNonNull(context, "context must not be null");
        if (durationNanos < 0) {
            throw new IllegalArgumentException("durationNanos must be >= 0");
        }
        appliedRules = List.copyOf(appliedRules);
    }
}

