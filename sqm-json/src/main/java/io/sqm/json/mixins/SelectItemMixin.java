package io.sqm.json.mixins;

/* ======================
 * SelectItem polymorphism
 * ====================== */

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.ExprSelectItem;
import io.sqm.core.QualifiedStarSelectItem;
import io.sqm.core.StarSelectItem;

/**
 * Jackson mixin root for select-item polymorphism.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ExprSelectItem.Impl.class, name = "expr"),
    @JsonSubTypes.Type(value = StarSelectItem.Impl.class, name = "star"),
    @JsonSubTypes.Type(value = QualifiedStarSelectItem.Impl.class, name = "qualified_star")
})
public abstract class SelectItemMixin extends CommonJsonMixin {

    /**
     * Creates select-item mixin metadata.
     */
    protected SelectItemMixin() {
    }
}
