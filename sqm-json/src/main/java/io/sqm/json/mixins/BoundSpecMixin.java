package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.internal.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = BoundSpecCurrentRow.class, name = "currentRow"),
    @JsonSubTypes.Type(value = BoundSpecFollowing.class, name = "following"),
    @JsonSubTypes.Type(value = BoundSpecPreceding.class, name = "preceding"),
    @JsonSubTypes.Type(value = BoundSpecUnboundedFollowing.class, name = "unboundedFollowing"),
    @JsonSubTypes.Type(value = BoundSpecUnboundedPreceding.class, name = "unboundedPreceding")
})
public class BoundSpecMixin extends CommonJsonMixin {
}
