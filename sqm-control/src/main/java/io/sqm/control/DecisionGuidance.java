package io.sqm.control;

import java.io.Serializable;

/**
 * Structured guidance for automated retry and remediation flows.
 *
 * @param retryable            whether an automated retry is expected to succeed
 * @param remediationHint      human-readable remediation suggestion
 * @param suggestedAction      short action identifier for agents (for example, {@code remove_ddl})
 * @param retryInstructionHint compact instruction that can be fed back to an LLM retry loop
 */
public record DecisionGuidance(
    boolean retryable,
    String remediationHint,
    String suggestedAction,
    String retryInstructionHint
) implements Serializable {

    /**
     * Creates a retryable guidance instance.
     *
     * @param remediationHint      remediation suggestion
     * @param suggestedAction      short action identifier
     * @param retryInstructionHint compact retry instruction
     * @return new guidance instance
     */
    public static DecisionGuidance retryable(String remediationHint, String suggestedAction, String retryInstructionHint) {
        return new DecisionGuidance(true, remediationHint, suggestedAction, retryInstructionHint);
    }

    /**
     * Creates a non-retryable guidance instance.
     *
     * @param remediationHint remediation suggestion
     * @param suggestedAction short action identifier
     * @return new guidance instance
     */
    public static DecisionGuidance terminal(String remediationHint, String suggestedAction) {
        return new DecisionGuidance(false, remediationHint, suggestedAction, null);
    }

    /**
     * Validates mandatory fields.
     *
     * @param retryable            whether retry is expected to succeed
     * @param remediationHint      remediation suggestion
     * @param suggestedAction      short action identifier
     * @param retryInstructionHint compact retry instruction
     */
    public DecisionGuidance {
        if (remediationHint == null || remediationHint.isBlank()) {
            throw new IllegalArgumentException("remediationHint must not be blank");
        }
        if (suggestedAction == null || suggestedAction.isBlank()) {
            throw new IllegalArgumentException("suggestedAction must not be blank");
        }
        if (retryable && (retryInstructionHint == null || retryInstructionHint.isBlank())) {
            throw new IllegalArgumentException("retryInstructionHint must not be blank for retryable guidance");
        }
    }
}
