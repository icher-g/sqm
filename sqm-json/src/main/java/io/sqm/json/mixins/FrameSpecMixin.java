package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.internal.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = FrameSpecBetween.class, name = "between"),
    @JsonSubTypes.Type(value = FrameSpecSingle.class, name = "single")
})
public class FrameSpecMixin extends CommonJsonMixin {
}
