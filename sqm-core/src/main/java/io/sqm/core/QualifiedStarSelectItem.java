package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * Represents a qualified '*' in a SELECT statement.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     SELECT t.*
 *     }
 * </pre>
 */
public non-sealed interface QualifiedStarSelectItem extends SelectItem {

    /**
     * Creates a qualified '*' item for the SELECT statement.
     *
     * @param qualifier a qualifier identifier. For example: {@code t.*}
     * @return {@link QualifiedStarSelectItem}.
     */
    static QualifiedStarSelectItem of(Identifier qualifier) {
        return new Impl(qualifier);
    }

    /**
     * Gets a qualifier before the '*'.
     *
     * @return a qualifier.
     */
    Identifier qualifier();

    /**
     * Accepts a {@link NodeVisitor} and dispatches control to the
     * visitor method corresponding to the concrete subtype
     *
     * @param v   the visitor instance to accept (must not be {@code null})
     * @param <R> the result type returned by the visitor
     * @return the result produced by the visitor
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitQualifiedStarSelectItem(this);
    }

    /**
     * Implements a qualified '*' in a SELECT statement.
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     SELECT t.*
     *     }
     * </pre>
     *
     * @param qualifier a qualifier before the '*'.
     */
    record Impl(Identifier qualifier) implements QualifiedStarSelectItem {
        /**
         * Creates a qualified-star select item.
         *
         * @param qualifier a qualifier identifier before the '*'
         */
        public Impl {
            java.util.Objects.requireNonNull(qualifier, "qualifier");
        }
    }
}
