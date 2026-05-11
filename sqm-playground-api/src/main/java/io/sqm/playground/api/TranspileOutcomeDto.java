package io.sqm.playground.api;

/**
 * Outcome classification for a transpilation request.
 */
public enum TranspileOutcomeDto {
    /**
     * exact transpilation
     */
    exact,
    /**
     * approximate transpilation, for example 'Value' ilike 'lookup' is converted to low('value') like low('lookup')
     */
    approximate,
    /**
     * transpilation is not supported for the current query
     */
    unsupported;

    /**
     * Resolves an outcome from its lowercase API value.
     *
     * @param value lowercase API value
     * @return matching outcome
     */
    public static TranspileOutcomeDto fromValue(String value) {
        try {
            return TranspileOutcomeDto.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown transpile outcome: " + value, e);
        }
    }
}
