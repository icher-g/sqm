package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Jackson mixin root for group-item polymorphism.
 */
public abstract class GroupItemMixin extends CommonJsonMixin {

    /**
     * Creates group-item mixin metadata.
     */
    protected GroupItemMixin() {
    }

    // Prevent helpers from leaking into JSON (adjust if your method name differs)
    /**
     * Indicates whether this grouping item originated from an ordinal position.
     *
     * @return {@code true} when ordinal grouping is used
     */
    @JsonIgnore
    boolean isOrdinal() { return false; }
}
