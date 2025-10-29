package io.sqm.core;

import io.sqm.core.internal.StarSelectItemImpl;

/**
 * Represent a '*' in SELECT. {@code SELECT *}.
 */
public non-sealed interface StarSelectItem extends SelectItem {

    /**
     * Creates a '*' placeholder in a SELECT statement.
     *
     * @return {@link StarSelectItem}.
     */
    static StarSelectItem of() {
        return new StarSelectItemImpl();
    }
}
