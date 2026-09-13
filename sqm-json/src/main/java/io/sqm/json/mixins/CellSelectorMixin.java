package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.*;

/** Jackson metadata for CellSelector variants. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CellSelector.Value.class, name = "cell-selector-value"),
    @JsonSubTypes.Type(value = CellSelector.Condition.class, name = "cell-selector-condition")
})
public abstract class CellSelectorMixin extends CommonJsonMixin {
    /** Creates mixin metadata. */
    protected CellSelectorMixin() {
    }
}
