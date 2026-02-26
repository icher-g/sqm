package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.FrameSpec;

/**
 * Jackson mixin root for window frame-spec polymorphism.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = FrameSpec.Between.Impl.class, name = "between"),
    @JsonSubTypes.Type(value = FrameSpec.Single.Impl.class, name = "single")
})
public class FrameSpecMixin extends CommonJsonMixin {

    /**
     * Creates frame-spec mixin metadata.
     */
    public FrameSpecMixin() {
    }
}
