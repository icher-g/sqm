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
 */
public record DecisionResult(
    DecisionKind kind,
    ReasonCode reasonCode,
    String message,
    String rewrittenSql
) implements Serializable {

    /**
     * Creates an allow decision with no specific reason.
     *
     * @return allow decision
     */
    public static DecisionResult allow() {
        return new DecisionResult(DecisionKind.ALLOW, ReasonCode.NONE, null, null);
    }

    /**
     * Creates a deny decision.
     *
     * @param reasonCode machine-readable reason code
     * @param message    human-readable message
     * @return deny decision
     */
    public static DecisionResult deny(ReasonCode reasonCode, String message) {
        return new DecisionResult(DecisionKind.DENY, reasonCode, message, null);
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
        return new DecisionResult(DecisionKind.REWRITE, reasonCode, message, rewrittenSql);
    }

    /**
     * Validates required fields and coherence rules.
     *
     * @param kind         decision kind
     * @param reasonCode   machine-readable reason code
     * @param message      human-readable message
     * @param rewrittenSql rewritten SQL
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
    }
}
