package io.sqm.middleware.api;

/**
 * Transport-neutral decision result.
 *
 * @param kind                decision kind
 * @param reasonCode          decision reason code
 * @param message             decision message
 * @param rewrittenSql        optional rewritten SQL
 * @param sqlParams           SQL parameters for rewritten SQL
 * @param fingerprint         optional canonical fingerprint
 * @param guidance            optional retry/remediation guidance
 */
public record DecisionResultDto(
    DecisionKindDto kind,
    ReasonCodeDto reasonCode,
    String message,
    String rewrittenSql,
    java.util.List<Object> sqlParams,
    String fingerprint,
    DecisionGuidanceDto guidance
) {
}
