package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.CellFor;
import io.sqm.core.CellSelector;

/**
 * Jackson metadata for CellAddress variants.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CellSelector.Value.class, name = "cell-selector-value"),
    @JsonSubTypes.Type(value = CellSelector.Condition.class, name = "cell-selector-condition"),
    @JsonSubTypes.Type(value = CellFor.Values.class, name = "cell-for-values"),
    @JsonSubTypes.Type(value = CellFor.Range.class, name = "cell-for-range")
})
public abstract class CellAddressMixin extends CommonJsonMixin {
    /**
     * Creates mixin metadata.
     */
    protected CellAddressMixin() {
    }
}
