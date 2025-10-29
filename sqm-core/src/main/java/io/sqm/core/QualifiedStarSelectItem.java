package io.sqm.core;

import io.sqm.core.internal.QualifiedStarSelectItemImpl;

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
     * @param qualifier a qualifier. For example: {@code t.*}
     * @return {@link QualifiedStarSelectItem}.
     */
    static QualifiedStarSelectItem of(String qualifier) {
        return new QualifiedStarSelectItemImpl(qualifier);
    }

    /**
     * Gets a qualifier before the '*'.
     *
     * @return a qualifier.
     */
    String qualifier();
}
