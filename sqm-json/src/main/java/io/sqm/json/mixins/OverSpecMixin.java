package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.OverSpec;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OverSpec.Def.Impl.class, name = "overDef"),
    @JsonSubTypes.Type(value = OverSpec.Ref.Impl.class, name = "overRef"),
})
public class OverSpecMixin extends CommonJsonMixin {
}
