package io.sqm.middleware.api;

/**
 * Transport-neutral SQL parameterization mode.
 */
public enum ParameterizationModeDto {
    /**
     * Do not parameterize inline literals.
     */
    OFF,

    /**
     * Parameterize inline literals using dialect placeholders.
     */
    BIND
}
