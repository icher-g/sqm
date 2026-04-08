package io.sqm.playground.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Outcome classification for a transpilation request.
 */
public enum TranspileOutcomeDto {
    EXACT("exact"),
    APPROXIMATE("approximate"),
    UNSUPPORTED("unsupported");

    private final String value;

    /**
     * Creates an outcome with its wire-format value.
     *
     * @param value lowercase API value
     */
    TranspileOutcomeDto(String value) {
        this.value = value;
    }

    /**
     * Returns the lowercase API value.
     *
     * @return lowercase API value
     */
    @JsonValue
    public String value() {
        return value;
    }

    /**
     * Resolves an outcome from its lowercase API value.
     *
     * @param value lowercase API value
     * @return matching outcome
     */
    @JsonCreator
    public static TranspileOutcomeDto fromValue(String value) {
        return Arrays.stream(values())
            .filter(candidate -> candidate.value.equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown transpile outcome: " + value));
    }
}
