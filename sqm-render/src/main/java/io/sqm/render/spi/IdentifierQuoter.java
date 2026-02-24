package io.sqm.render.spi;

import io.sqm.core.QuoteStyle;

/**
 * An interface to customize the way the identifier is quoted.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     "Order" | [Order] | `Order`
 *     }
 * </pre>
 */
public interface IdentifierQuoter {
    /**
     * Puts the identifier inside the quotes supported by the dialect.
     *
     * @param identifier the identifier to quote.
     * @return a quoted identifier.
     */
    String quote(String identifier);

    /**
     * Puts the identifier inside the requested quote style if supported by the dialect.
     * <p>If the style is not supported the implementation may throw an exception. Callers may use
     * {@link #supports(QuoteStyle)} to decide whether to preserve the original style or to fall back
     * to the dialect default via {@link #quote(String)}.</p>
     *
     * @param identifier the identifier to quote.
     * @param quoteStyle the requested quote style.
     * @return a quoted identifier.
     */
    default String quote(String identifier, QuoteStyle quoteStyle) {
        if (quoteStyle == null || quoteStyle == QuoteStyle.NONE) {
            return quoteIfNeeded(identifier);
        }
        return quote(identifier);
    }

    /**
     * Puts the identifier inside the quotes supported by the dialect only if needed.
     * The possible implementation is to check whether the identifier is a keyword and only then to quote it.
     *
     * @param identifier the identifier to quote.
     * @return a quoted or an original identifier.
     */
    String quoteIfNeeded(String identifier);

    /**
     * Quotes each part of the identifier separately.
     * <p>Example:</p>
     * <pre>
     *      {@code
     *      "s"."t" | [s].[t]
     *      }
     *  </pre>
     *
     * @param schemaOrNull the name of the schema if presented or nULL.
     * @param name         the name.
     * @return a quoted qualified name.
     */
    String qualify(String schemaOrNull, String name);

    /**
     * Indicates if the provided identifier needs to be quoted to avoid ambiguity.
     *
     * @param identifier the identifier.
     * @return True if the identifier must be quoted or False otherwise.
     */
    boolean needsQuoting(String identifier);

    /**
     * Indicates whether the dialect supports a specific identifier quote style.
     * <p>The default implementation assumes SQL-standard double quotes only.</p>
     *
     * @param quoteStyle the quote style to check.
     * @return True if supported or False otherwise.
     */
    default boolean supports(QuoteStyle quoteStyle) {
        return quoteStyle == null || quoteStyle == QuoteStyle.NONE || quoteStyle == QuoteStyle.DOUBLE_QUOTE;
    }
}
