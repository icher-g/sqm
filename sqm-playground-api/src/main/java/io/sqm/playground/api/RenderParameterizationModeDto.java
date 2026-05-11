package io.sqm.playground.api;

/**
 * Literal parameterization modes supported by playground render requests.
 */
public enum RenderParameterizationModeDto {
    /**
     * keep all parameters as part of the SQL itself
     */
    inline,
    /**
     * extract parameters from the query and keep them outside
     */
    bind;

    /**
     * Resolves a parameterization mode from its lowercase API value.
     *
     * @param value lowercase API value
     * @return matching parameterization mode
     */
    public static RenderParameterizationModeDto fromValue(String value) {
        try {
            return RenderParameterizationModeDto.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown render parameterization mode: " + value, e);
        }
    }
}
