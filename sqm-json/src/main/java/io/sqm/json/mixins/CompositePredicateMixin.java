package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.internal.AndPredicateImpl;
import io.sqm.core.internal.OrPredicateImpl;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AndPredicateImpl.class, name = "and"),
    @JsonSubTypes.Type(value = OrPredicateImpl.class, name = "or")
})
public abstract class CompositePredicateMixin extends CommonJsonMixin {
}
