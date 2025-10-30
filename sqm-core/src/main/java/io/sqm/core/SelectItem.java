package io.sqm.core;

import io.sqm.core.internal.ExprSelectItemImpl;
import io.sqm.core.internal.QualifiedStarSelectItemImpl;
import io.sqm.core.internal.StarSelectItemImpl;

import java.util.Optional;

/**
 * A single item in the SELECT list.
 */
public sealed interface SelectItem extends Node
    permits ExprSelectItem, StarSelectItem, QualifiedStarSelectItem {

    /**
     * Creates an expression wrapper for a SELECT statement.
     *
     * @param expr an expression to wrap.
     * @return A newly created instance of a wrapper.
     */
    static ExprSelectItem expr(Expression expr) {
        return new ExprSelectItemImpl(expr, null);
    }

    /**
     * Creates a '*' placeholder in a SELECT statement.
     *
     * @return {@link StarSelectItem}.
     */
    static StarSelectItem star() {
        return new StarSelectItemImpl();
    }

    /**
     * Creates a qualified '*' item for the SELECT statement.
     *
     * @param qualifier a qualifier. For example: {@code t.*}
     * @return {@link QualifiedStarSelectItem}.
     */
    static QualifiedStarSelectItem star(String qualifier) {
        return new QualifiedStarSelectItemImpl(qualifier);
    }

    /**
     * Casts current instance of select item to {@link ExprSelectItem} if possible.
     *
     * @return an {@link Optional}<{@link ExprSelectItem}>.
     */
    default Optional<ExprSelectItem> asExpr() {
        return this instanceof ExprSelectItem e ? Optional.of(e) : Optional.empty();
    }

    /**
     * Casts current instance of select item to {@link StarSelectItem} if possible.
     *
     * @return an {@link Optional}<{@link StarSelectItem}>.
     */
    default Optional<StarSelectItem> asStar() {
        return this instanceof StarSelectItem e ? Optional.of(e) : Optional.empty();
    }

    /**
     * Casts current instance of select item to {@link QualifiedStarSelectItem} if possible.
     *
     * @return an {@link Optional}<{@link QualifiedStarSelectItem}>.
     */
    default Optional<QualifiedStarSelectItem> asQualifiedStar() {
        return this instanceof QualifiedStarSelectItem e ? Optional.of(e) : Optional.empty();
    }
}
