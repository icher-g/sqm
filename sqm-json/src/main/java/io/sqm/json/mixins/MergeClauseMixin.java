package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.MergeClause;

/**
 * Jackson mixin root for {@link MergeClause} polymorphism.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = MergeClause.Impl.class, name = "mergeClause")
})
public abstract class MergeClauseMixin extends CommonJsonMixin {

    /**
     * Creates merge-clause mixin metadata.
     */
    protected MergeClauseMixin() {
    }
}
