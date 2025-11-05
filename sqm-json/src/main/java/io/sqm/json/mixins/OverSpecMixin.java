package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.internal.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OverSpecDef.class, name = "overDef"),
    @JsonSubTypes.Type(value = OverSpecRef.class, name = "overRef"),
})
public class OverSpecMixin extends CommonJsonMixin {
}
