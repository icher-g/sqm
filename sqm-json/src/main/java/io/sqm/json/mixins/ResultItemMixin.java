package io.sqm.json.mixins;

/* ======================
 * ResultItem polymorphism
 * ====================== */

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.ExprResultItem;
import io.sqm.core.OutputStarResultItem;
import io.sqm.core.QualifiedStarResultItem;
import io.sqm.core.StarResultItem;

/**
 * Jackson mixin root for select-item polymorphism.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ExprResultItem.Impl.class, name = "expr"),
    @JsonSubTypes.Type(value = OutputStarResultItem.Impl.class, name = "output_star"),
    @JsonSubTypes.Type(value = StarResultItem.Impl.class, name = "star"),
    @JsonSubTypes.Type(value = QualifiedStarResultItem.Impl.class, name = "qualified_star")
})
public abstract class ResultItemMixin extends CommonJsonMixin {

    /**
     * Creates select-item mixin metadata.
     */
    protected ResultItemMixin() {
    }
}
