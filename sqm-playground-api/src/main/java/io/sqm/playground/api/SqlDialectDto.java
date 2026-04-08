package io.sqm.playground.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Stable SQL dialect identifiers used by the playground API.
 */
public enum SqlDialectDto {
    ANSI("ansi"),
    POSTGRESQL("postgresql"),
    MYSQL("mysql"),
    SQLSERVER("sqlserver");

    private final String value;

    /**
     * Creates a dialect with its wire-format value.
     *
     * @param value lowercase API value
     */
    SqlDialectDto(String value) {
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
     * Resolves a dialect from its lowercase API value.
     *
     * @param value lowercase API value
     * @return matching dialect
     */
    @JsonCreator
    public static SqlDialectDto fromValue(String value) {
        return Arrays.stream(values())
            .filter(candidate -> candidate.value.equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown SQL dialect: " + value));
    }
}
