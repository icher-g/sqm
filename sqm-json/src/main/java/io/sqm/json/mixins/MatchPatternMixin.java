package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.MatchPattern;

/**
 * Jackson mixin root for typed match-pattern polymorphism.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = MatchPattern.Variable.class, name = "pattern-variable"),
    @JsonSubTypes.Type(value = MatchPattern.Sequence.class, name = "pattern-sequence"),
    @JsonSubTypes.Type(value = MatchPattern.Alternation.class, name = "pattern-alternation"),
    @JsonSubTypes.Type(value = MatchPattern.Permutation.class, name = "pattern-permutation"),
    @JsonSubTypes.Type(value = MatchPattern.Anchor.class, name = "pattern-anchor"),
    @JsonSubTypes.Type(value = MatchPattern.Empty.class, name = "pattern-empty"),
    @JsonSubTypes.Type(value = MatchPattern.Exclusion.class, name = "pattern-exclusion"),
    @JsonSubTypes.Type(value = MatchPattern.Quantified.class, name = "pattern-quantified")
})
public abstract class MatchPatternMixin extends CommonJsonMixin {
    /**
     * Creates match-pattern mixin metadata.
     */
    protected MatchPatternMixin() {
    }
}
