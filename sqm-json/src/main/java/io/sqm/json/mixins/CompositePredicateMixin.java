package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.AndPredicate;
import io.sqm.core.OrPredicate;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AndPredicate.Impl.class, name = "and"),
    @JsonSubTypes.Type(value = OrPredicate.Impl.class, name = "or")
})
public abstract class CompositePredicateMixin extends CommonJsonMixin {
}
