package io.sqm.json.mixins;

/* ======================
 * Predicate polymorphism
 * ====================== */

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.internal.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AnyAllPredicateImpl.class, name = "any_all"),
    @JsonSubTypes.Type(value = BetweenPredicateImpl.class, name = "between"),
    @JsonSubTypes.Type(value = ComparisonPredicateImpl.class, name = "comparison"),
    @JsonSubTypes.Type(value = ExistsPredicateImpl.class, name = "exists"),
    @JsonSubTypes.Type(value = InPredicateImpl.class, name = "in"),
    @JsonSubTypes.Type(value = IsNullPredicateImpl.class, name = "is_null"),
    @JsonSubTypes.Type(value = LikePredicateImpl.class, name = "like"),
    @JsonSubTypes.Type(value = NotPredicateImpl.class, name = "not"),
    @JsonSubTypes.Type(value = UnaryPredicateImpl.class, name = "unary")
})
public abstract class PredicateMixin extends CommonJsonMixin {
}
