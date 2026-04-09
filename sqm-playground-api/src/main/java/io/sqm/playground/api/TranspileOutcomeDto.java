package io.sqm.playground.api;

/**
 * Outcome classification for a transpilation request.
 */
public enum TranspileOutcomeDto {
    exact,
    approximate,
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
