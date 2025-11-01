package io.sqm.core.walk;

import io.sqm.core.ExprSelectItem;
import io.sqm.core.QualifiedStarSelectItem;
import io.sqm.core.SelectItem;
import io.sqm.core.StarSelectItem;

/**
 * Visitor interface for traversing {@code SELECT} list items.
 * <p>
 * This visitor provides methods for visiting different forms
 * of select items, such as expressions, unqualified stars,
 * or qualified star references. It allows external logic to
 * render, analyze, or transform projection lists.
 * </p>
 *
 * @param <R> the result type produced by the visitor methods,
 *            or {@link Void} if the visitor performs only side effects
 *
 * @see SelectItem
 * @see ExprSelectItem
 * @see StarSelectItem
 * @see QualifiedStarSelectItem
 */
public interface SelectItemVisitor<R> {

    /**
     * Visits an {@link ExprSelectItem}, representing a standard
     * projected expression, possibly with an alias.
     * Example: {@code LOWER(u.name) AS username}
     *
     * @param i the expression select item being visited (never {@code null})
     * @return a result value, or {@code null} if {@code <R>} is {@link Void}
     */
    R visitExprSelectItem(ExprSelectItem i);

    /**
     * Visits a {@link StarSelectItem}, representing an unqualified
     * {@code *} projection.
     * Example: {@code SELECT * FROM users}
     *
     * @param i the star select item being visited (never {@code null})
     * @return a result value, or {@code null} if {@code <R>} is {@link Void}
     */
    R visitStarSelectItem(StarSelectItem i);

    /**
     * Visits a {@link QualifiedStarSelectItem}, representing a
     * qualified {@code table.*} projection.
     * Example: {@code SELECT u.* FROM users u}
     *
     * @param i the qualified star select item being visited (never {@code null})
     * @return a result value, or {@code null} if {@code <R>} is {@link Void}
     */
    R visitQualifiedStarSelectItem(QualifiedStarSelectItem i);
}

