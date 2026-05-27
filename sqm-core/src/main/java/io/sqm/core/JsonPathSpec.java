package io.sqm.core;

import java.io.Serializable;
import java.util.Objects;

/**
 * Opaque SQL/JSON path text preserved by the SQM model.
 *
 * @param text JSON path text without SQL string literal delimiters
 */
public record JsonPathSpec(String text) implements Serializable {
    /**
     * Creates a JSON path specification.
     *
     * @param text JSON path text without SQL string literal delimiters
     */
    public JsonPathSpec {
        if (Objects.requireNonNull(text, "text").isBlank()) {
            throw new IllegalArgumentException("JSON path text must not be blank");
        }
    }

    /**
     * Creates a JSON path specification.
     *
     * @param text JSON path text without SQL string literal delimiters
     * @return JSON path specification
     */
    public static JsonPathSpec of(String text) {
        return new JsonPathSpec(text);
    }
}
