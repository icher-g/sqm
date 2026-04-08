package io.sqm.playground.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Identifies the processing phase that produced a playground diagnostic.
 */
public enum DiagnosticPhaseDto {
    HTTP("http"),
    PARSE("parse"),
    RENDER("render"),
    VALIDATE("validate"),
    TRANSPILE("transpile");

    private final String value;

    /**
     * Creates a phase with its wire-format value.
     *
     * @param value lowercase API value
     */
    DiagnosticPhaseDto(String value) {
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
     * Resolves a phase from its lowercase API value.
     *
     * @param value lowercase API value
     * @return matching phase
     */
    @JsonCreator
    public static DiagnosticPhaseDto fromValue(String value) {
        return Arrays.stream(values())
            .filter(candidate -> candidate.value.equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown diagnostic phase: " + value));
    }
}
