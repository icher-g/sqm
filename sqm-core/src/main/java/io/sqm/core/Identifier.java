package io.sqm.core;

import java.util.Objects;
import java.io.Serializable;

/**
 * Immutable SQL identifier value with preserved quote delimiter style.
 * <p>
 * The {@link #value()} stores the identifier text without quote delimiters.
 * Quote information is captured separately via {@link #quoteStyle()}.
 *
 * @param value identifier text without quote delimiters
 * @param quoteStyle original quote delimiter style (or {@link QuoteStyle#NONE})
 */
public record Identifier(String value, QuoteStyle quoteStyle) implements Serializable {
    /**
     * Creates an identifier and normalizes a {@code null} quote style to {@link QuoteStyle#NONE}.
     *
     * @param value      identifier text without delimiters
     * @param quoteStyle quote delimiter style or {@code null} for unquoted
     */
    public Identifier {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value");
        }
        quoteStyle = quoteStyle == null ? QuoteStyle.NONE : quoteStyle;
    }

    /**
     * Creates an unquoted identifier.
     *
     * @param value identifier text
     * @return unquoted identifier
     */
    public static Identifier of(String value) {
        return new Identifier(value, QuoteStyle.NONE);
    }

    /**
     * Creates an identifier with an explicit quote style.
     *
     * @param value      identifier text without delimiters
     * @param quoteStyle quote delimiter style
     * @return identifier instance
     */
    public static Identifier of(String value, QuoteStyle quoteStyle) {
        return new Identifier(value, quoteStyle);
    }

    /**
     * Indicates whether this identifier was quoted in the source SQL.
     *
     * @return {@code true} if a quote delimiter style is present
     */
    public boolean quoted() {
        return quoteStyle != QuoteStyle.NONE;
    }
}
