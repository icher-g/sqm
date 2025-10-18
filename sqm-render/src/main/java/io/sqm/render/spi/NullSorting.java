package io.sqm.render.spi;

import io.sqm.core.Direction;
import io.sqm.core.Nulls;

/**
 * An interface to support NULL customizations in an OrderBy statement.
 */
public interface NullSorting {
    /**
     * Indicates whether explicit syntax is supported by the dialect.
     *
     * @return True if the explicit syntax is supported or False otherwise.
     */
    boolean supportsExplicit();

    /**
     * Gets a string representation of a {@link Nulls} enum value.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     NULLS FIRST | NULLS LAST
     *     }
     * </pre>
     *
     * @param n an enum value.
     * @return a string representation of "\"s\".\"t\"" | "[s].[t]".
     */
    String keyword(Nulls n);

    /**
     * Gets a default {@link Nulls} value for a {@link Direction} in a dialect.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     ASC -> LAST, DESC -> FIRST (typical)
     *     }
     * </pre>
     *
     * @param dir a direction to supply the default for.
     * @return a default.
     */
    Nulls defaultFor(Direction dir);
}
