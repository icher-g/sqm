package io.sqm.middleware.api;

/**
 * Transport-neutral guidance for retry and remediation flows.
 *
 * @param retryable            whether automated retry is expected to succeed
 * @param remediationHint      human-readable remediation suggestion
 * @param suggestedAction      short action identifier
 * @param retryInstructionHint compact instruction suitable for retry loops
 */
public record DecisionGuidanceDto(
    boolean retryable,
    String remediationHint,
    String suggestedAction,
    String retryInstructionHint
) {
}
