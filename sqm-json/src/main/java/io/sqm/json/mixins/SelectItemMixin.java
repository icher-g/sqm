package io.sqm.json.mixins;

/* ======================
 * SelectItem polymorphism
 * ====================== */

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.internal.ExprSelectItemImpl;
import io.sqm.core.internal.QualifiedStarSelectItemImpl;
import io.sqm.core.internal.StarSelectItemImpl;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ExprSelectItemImpl.class, name = "expr"),
    @JsonSubTypes.Type(value = StarSelectItemImpl.class, name = "star"),
    @JsonSubTypes.Type(value = QualifiedStarSelectItemImpl.class, name = "qualified_star")
})
public abstract class SelectItemMixin extends CommonJsonMixin {
}
