package io.sqm.playground.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Identifies the severity level of a playground diagnostic.
 */
public enum DiagnosticSeverityDto {
    ERROR("error"),
    WARNING("warning"),
    INFO("info");

    private final String value;

    /**
     * Creates a severity with its wire-format value.
     *
     * @param value lowercase API value
     */
    DiagnosticSeverityDto(String value) {
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
     * Resolves a severity from its lowercase API value.
     *
     * @param value lowercase API value
     * @return matching severity
     */
    @JsonCreator
    public static DiagnosticSeverityDto fromValue(String value) {
        return Arrays.stream(values())
            .filter(candidate -> candidate.value.equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown diagnostic severity: " + value));
    }
}
