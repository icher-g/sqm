package io.sqm.middleware.api;

/**
 * Transport-neutral decision explanation.
 *
 * @param decision    decision result
 * @param explanation explanation text
 */
public record DecisionExplanationDto(DecisionResultDto decision, String explanation) {
}
