package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.MergeDeleteAction;
import io.sqm.core.MergeDoNothingAction;
import io.sqm.core.MergeInsertAction;
import io.sqm.core.MergeUpdateAction;

/**
 * Jackson mixin root for MERGE action polymorphism.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = MergeUpdateAction.Impl.class, name = "mergeUpdateAction"),
    @JsonSubTypes.Type(value = MergeDeleteAction.Impl.class, name = "mergeDeleteAction"),
    @JsonSubTypes.Type(value = MergeDoNothingAction.Impl.class, name = "mergeDoNothingAction"),
    @JsonSubTypes.Type(value = MergeInsertAction.Impl.class, name = "mergeInsertAction")
})
public abstract class MergeActionMixin extends CommonJsonMixin {

    /**
     * Creates merge-action mixin metadata.
     */
    protected MergeActionMixin() {
    }
}
