package io.cherlabs.sqm.render.spi;

/**
 * An interface to customise the way the identifier is quoted.
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
}
