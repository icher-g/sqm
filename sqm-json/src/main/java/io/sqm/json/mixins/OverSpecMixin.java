package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.OverSpec;

/**
 * Jackson mixin root for window over-spec polymorphism.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OverSpec.Def.Impl.class, name = "overDef"),
    @JsonSubTypes.Type(value = OverSpec.Ref.Impl.class, name = "overRef"),
})
public class OverSpecMixin extends CommonJsonMixin {

    /**
     * Creates over-spec mixin metadata.
     */
    public OverSpecMixin() {
    }
}
