package io.sqm.core.internal;

import io.sqm.core.QualifiedStarSelectItem;

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
public record QualifiedStarSelectItemImpl(String qualifier) implements QualifiedStarSelectItem {
}
