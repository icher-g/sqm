package io.sqm.json.mixins;

/* ======================
 * Predicate polymorphism
 * ====================== */

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.*;

/**
 * Jackson mixin root for predicate polymorphism.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AndPredicate.Impl.class, name = "and"),
    @JsonSubTypes.Type(value = AnyAllPredicate.Impl.class, name = "any_all"),
    @JsonSubTypes.Type(value = BetweenPredicate.Impl.class, name = "between"),
    @JsonSubTypes.Type(value = ComparisonPredicate.Impl.class, name = "comparison"),
    @JsonSubTypes.Type(value = ExistsPredicate.Impl.class, name = "exists"),
    @JsonSubTypes.Type(value = InPredicate.Impl.class, name = "in"),
    @JsonSubTypes.Type(value = IsNullPredicate.Impl.class, name = "is_null"),
    @JsonSubTypes.Type(value = LikePredicate.Impl.class, name = "like"),
    @JsonSubTypes.Type(value = RegexPredicate.Impl.class, name = "regex"),
    @JsonSubTypes.Type(value = NotPredicate.Impl.class, name = "not"),
    @JsonSubTypes.Type(value = OrPredicate.Impl.class, name = "or"),
    @JsonSubTypes.Type(value = UnaryPredicate.Impl.class, name = "unary")
})
public abstract class PredicateMixin extends CommonJsonMixin {

    /**
     * Creates predicate mixin metadata.
     */
    protected PredicateMixin() {
    }
}
