package io.sqm.core.control;

import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable decision payload returned by SQL middleware processing.
 *
 * @param kind          decision kind
 * @param reasonCode    machine-readable reason code
 * @param message       human-readable message, may be {@code null}
 * @param rewrittenSql  rewritten SQL when {@link DecisionKind#REWRITE}, otherwise {@code null}
 * @param guidance      machine-actionable retry/remediation guidance, required for deny decisions
 */
public record DecisionResult(
    DecisionKind kind,
    ReasonCode reasonCode,
    String message,
    String rewrittenSql,
    DecisionGuidance guidance
) implements Serializable {

    /**
     * Creates an allow decision with no specific reason.
     *
     * @return allow decision
     */
    public static DecisionResult allow() {
        return new DecisionResult(DecisionKind.ALLOW, ReasonCode.NONE, null, null, null);
    }

    /**
     * Creates a deny decision.
     *
     * @param reasonCode machine-readable reason code
     * @param message    human-readable message
     * @return deny decision
     */
    public static DecisionResult deny(ReasonCode reasonCode, String message) {
        return deny(reasonCode, message, ReasonGuidanceCatalog.forReason(reasonCode));
    }

    /**
     * Creates a deny decision with explicit guidance.
     *
     * @param reasonCode machine-readable reason code
     * @param message    human-readable message
     * @param guidance   machine-actionable retry/remediation guidance
     * @return deny decision
     */
    public static DecisionResult deny(ReasonCode reasonCode, String message, DecisionGuidance guidance) {
        return new DecisionResult(DecisionKind.DENY, reasonCode, message, null, guidance);
    }

    /**
     * Creates a rewrite decision.
     *
     * @param reasonCode   machine-readable rewrite reason
     * @param message      human-readable message
     * @param rewrittenSql rewritten SQL
     * @return rewrite decision
     */
    public static DecisionResult rewrite(ReasonCode reasonCode, String message, String rewrittenSql) {
        return new DecisionResult(DecisionKind.REWRITE, reasonCode, message, rewrittenSql, null);
    }

    /**
     * Validates required fields and coherence rules.
     *
     * @param kind         decision kind
     * @param reasonCode   machine-readable reason code
     * @param message      human-readable message
     * @param rewrittenSql rewritten SQL
     * @param guidance     machine-actionable retry/remediation guidance
     */
    public DecisionResult {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(reasonCode, "reasonCode must not be null");

        if (kind == DecisionKind.ALLOW && reasonCode != ReasonCode.NONE) {
            throw new IllegalArgumentException("ALLOW decision must use ReasonCode.NONE");
        }
        if (kind == DecisionKind.REWRITE && (rewrittenSql == null || rewrittenSql.isBlank())) {
            throw new IllegalArgumentException("REWRITE decision requires rewrittenSql");
        }
        if (kind != DecisionKind.REWRITE && rewrittenSql != null) {
            throw new IllegalArgumentException("rewrittenSql is only allowed for REWRITE decisions");
        }
        if (kind == DecisionKind.DENY && guidance == null) {
            throw new IllegalArgumentException("DENY decision requires guidance");
        }
        if (kind != DecisionKind.DENY && guidance != null) {
            throw new IllegalArgumentException("guidance is only allowed for DENY decisions");
        }
    }
}
