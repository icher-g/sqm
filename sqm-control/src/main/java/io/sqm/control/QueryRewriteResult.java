package io.sqm.control;

import io.sqm.core.Query;

import java.util.List;
import java.util.Objects;

/**
 * Result of applying one or more query rewrite rules.
 *
 * @param query           resulting query model after rewrite evaluation
 * @param rewritten       whether any rewrite was applied
 * @param appliedRuleIds  identifiers of applied rewrite rules in application order
 */
public record QueryRewriteResult(
    Query query,
    boolean rewritten,
    List<String> appliedRuleIds,
    ReasonCode primaryReasonCode
) {

    /**
     * Creates a result representing no rewrite.
     *
     * @param query query model
     * @return unchanged rewrite result
     */
    public static QueryRewriteResult unchanged(Query query) {
        return new QueryRewriteResult(query, false, List.of(), ReasonCode.NONE);
    }

    /**
     * Creates a result representing a rewrite by a single rule.
     *
     * @param query   rewritten query model
     * @param ruleId  applied rule identifier
     * @param reasonCode primary rewrite reason code
     * @return rewritten result
     */
    public static QueryRewriteResult rewritten(Query query, String ruleId, ReasonCode reasonCode) {
        return new QueryRewriteResult(query, true, List.of(ruleId), reasonCode);
    }

    /**
     * Validates and normalizes the rewrite result.
     *
     * @param query          resulting query model
     * @param rewritten      whether any rewrite was applied
     * @param appliedRuleIds identifiers of applied rewrite rules
     * @param primaryReasonCode primary reason code for rewrite decisions
     */
    public QueryRewriteResult {
        Objects.requireNonNull(query, "query must not be null");
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
}
