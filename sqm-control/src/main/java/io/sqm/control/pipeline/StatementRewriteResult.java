package io.sqm.control.pipeline;

import io.sqm.control.decision.ReasonCode;
import io.sqm.core.Statement;

import java.util.List;
import java.util.Objects;

/**
 * Result of applying one or more statement rewrite rules.
 *
 * @param statement         resulting statement model after rewrite evaluation
 * @param rewritten         whether any rewrite was applied
 * @param appliedRuleIds    identifiers of applied rewrite rules in application order
 * @param primaryReasonCode primary rewrite reason code, or {@link ReasonCode#NONE} when unchanged
 */
public record StatementRewriteResult(
    Statement statement,
    boolean rewritten,
    List<String> appliedRuleIds,
    ReasonCode primaryReasonCode
) {

    /**
     * Validates and normalizes the rewrite result.
     *
     * @param statement         resulting statement model
     * @param rewritten         whether any rewrite was applied
     * @param appliedRuleIds    identifiers of applied rewrite rules
     * @param primaryReasonCode primary reason code for rewrite decisions
     */
    public StatementRewriteResult {
        Objects.requireNonNull(statement, "statement must not be null");
        Objects.requireNonNull(appliedRuleIds, "appliedRuleIds must not be null");
        Objects.requireNonNull(primaryReasonCode, "primaryReasonCode must not be null");
        appliedRuleIds = List.copyOf(appliedRuleIds);
        for (String appliedRuleId : appliedRuleIds) {
            if (appliedRuleId == null || appliedRuleId.isBlank()) {
                throw new IllegalArgumentException("appliedRuleIds must not contain blank values");
            }
        }
        if (!rewritten && !appliedRuleIds.isEmpty()) {
            throw new IllegalArgumentException("appliedRuleIds must be empty when rewritten is false");
        }
        if (!rewritten && primaryReasonCode != ReasonCode.NONE) {
            throw new IllegalArgumentException("primaryReasonCode must be NONE when rewritten is false");
        }
        if (rewritten && appliedRuleIds.isEmpty()) {
            throw new IllegalArgumentException("appliedRuleIds must not be empty when rewritten is true");
        }
        if (rewritten && primaryReasonCode == ReasonCode.NONE) {
            throw new IllegalArgumentException("primaryReasonCode must not be NONE when rewritten is true");
        }
    }

    /**
     * Creates a result representing no rewrite.
     *
     * @param statement statement model
     * @return unchanged rewrite result
     */
    public static StatementRewriteResult unchanged(Statement statement) {
        return new StatementRewriteResult(statement, false, List.of(), ReasonCode.NONE);
    }

    /**
     * Creates a result representing a rewrite by a single rule.
     *
     * @param statement  rewritten statement model
     * @param ruleId     applied rule identifier
     * @param reasonCode primary rewrite reason code
     * @return rewritten result
     */
    public static StatementRewriteResult rewritten(Statement statement, String ruleId, ReasonCode reasonCode) {
        return new StatementRewriteResult(statement, true, List.of(ruleId), reasonCode);
    }
}



