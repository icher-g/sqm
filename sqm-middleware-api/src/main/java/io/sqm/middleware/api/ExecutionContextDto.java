package io.sqm.middleware.api;

/**
 * Transport-neutral execution context payload.
 *
 * @param dialect              SQL dialect identifier
 * @param principal            optional principal identifier
 * @param tenant               optional tenant identifier
 * @param mode                 processing mode
 * @param parameterizationMode parameterization mode
 */
public record ExecutionContextDto(
    String dialect,
    String principal,
    String tenant,
    ExecutionModeDto mode,
    ParameterizationModeDto parameterizationMode
) {
}
