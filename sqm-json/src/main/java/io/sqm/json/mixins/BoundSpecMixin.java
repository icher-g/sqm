package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.BoundSpec;

/**
 * Jackson mixin root for window bound-spec polymorphism.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = BoundSpec.CurrentRow.Impl.class, name = "currentRow"),
    @JsonSubTypes.Type(value = BoundSpec.Following.Impl.class, name = "following"),
    @JsonSubTypes.Type(value = BoundSpec.Preceding.Impl.class, name = "preceding"),
    @JsonSubTypes.Type(value = BoundSpec.UnboundedFollowing.Impl.class, name = "unboundedFollowing"),
    @JsonSubTypes.Type(value = BoundSpec.UnboundedPreceding.Impl.class, name = "unboundedPreceding")
})
public class BoundSpecMixin extends CommonJsonMixin {

    /**
     * Creates bound-spec mixin metadata.
     */
    public BoundSpecMixin() {
    }
}
