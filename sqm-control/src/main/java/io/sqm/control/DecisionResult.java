package io.sqm.control;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Immutable decision payload returned by SQL middleware processing.
 *
 * @param kind         decision kind
 * @param reasonCode   machine-readable reason code
 * @param message      human-readable message, may be {@code null}
 * @param rewrittenSql rewritten SQL when {@link DecisionKind#REWRITE}, otherwise {@code null}
 * @param sqlParams    a list of parameters bound into a {@code rewrittenSql}. The list can be empty if parameterization mode is set to {@link ParameterizationMode#OFF}.
 * @param fingerprint  canonical query fingerprint when available, otherwise {@code null}
 * @param guidance     machine-actionable retry/remediation guidance, required for deny decisions
 */
public record DecisionResult(
    DecisionKind kind,
    ReasonCode reasonCode,
    String message,
    String rewrittenSql,
    List<Object> sqlParams,
    String fingerprint,
    DecisionGuidance guidance
) implements Serializable {

    /**
     * Validates required fields and coherence rules.
     *
     * @param kind         decision kind
     * @param reasonCode   machine-readable reason code
     * @param message      human-readable message
     * @param rewrittenSql rewritten SQL
     * @param fingerprint  canonical query fingerprint
     * @param guidance     machine-actionable retry/remediation guidance
     */
    public DecisionResult {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(reasonCode, "reasonCode must not be null");
        Objects.requireNonNull(sqlParams, "sqlParams must not be null");

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
        if (fingerprint != null && fingerprint.isBlank()) {
            throw new IllegalArgumentException("fingerprint must not be blank");
        }
    }

    /**
     * Creates an allow decision with no specific reason.
     *
     * @return allow decision
     */
    public static DecisionResult allow() {
        return allow(null);
    }

    /**
     * Creates an allow decision with optional canonical fingerprint.
     *
     * @param fingerprint canonical fingerprint.
     * @return allow decision
     */
    public static DecisionResult allow(String fingerprint) {
        return new DecisionResult(DecisionKind.ALLOW, ReasonCode.NONE, null, null, List.of(), fingerprint, null);
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
        return new DecisionResult(DecisionKind.DENY, reasonCode, message, null, List.of(), null, guidance);
    }

    /**
     * Creates a rewrite decision.
     *
     * @param reasonCode   machine-readable rewrite reason
     * @param message      human-readable message
     * @param rewrittenSql rewritten SQL
     * @param sqlParams    a list of parameters bound into a {@code rewrittenSql}.
     * @return rewrite decision
     */
    public static DecisionResult rewrite(ReasonCode reasonCode, String message, String rewrittenSql, List<Object> sqlParams) {
        return rewrite(reasonCode, message, rewrittenSql, sqlParams, null);
    }

    /**
     * Creates a rewrite decision with canonical fingerprint.
     *
     * @param reasonCode   machine-readable rewrite reason
     * @param message      human-readable message
     * @param rewrittenSql rewritten SQL
     * @param sqlParams    a list of parameters bound into a {@code rewrittenSql}.
     * @param fingerprint  canonical fingerprint
     * @return rewrite decision
     */
    public static DecisionResult rewrite(ReasonCode reasonCode, String message, String rewrittenSql, List<Object> sqlParams, String fingerprint) {
        return new DecisionResult(DecisionKind.REWRITE, reasonCode, message, rewrittenSql, sqlParams, fingerprint, null);
    }

    /**
     * Returns a copy of this decision with canonical fingerprint attached.
     *
     * @param fingerprint canonical fingerprint value.
     * @return decision copy with fingerprint.
     */
    public DecisionResult withFingerprint(String fingerprint) {
        return new DecisionResult(kind, reasonCode, message, rewrittenSql, sqlParams, fingerprint, guidance);
    }
}
