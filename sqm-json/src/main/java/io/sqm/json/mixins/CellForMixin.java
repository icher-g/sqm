package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.*;

/** Jackson metadata for CellFor variants. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CellFor.Values.class, name = "cell-for-values"),
    @JsonSubTypes.Type(value = CellFor.Range.class, name = "cell-for-range")
})
public abstract class CellForMixin extends CommonJsonMixin {
    /** Creates mixin metadata. */
    protected CellForMixin() {
    }
}
