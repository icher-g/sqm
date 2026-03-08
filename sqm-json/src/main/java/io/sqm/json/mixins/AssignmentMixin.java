package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.Assignment;

/**
 * Jackson mixin root for assignment polymorphism.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Assignment.Impl.class, name = "assignment")
})
public abstract class AssignmentMixin extends CommonJsonMixin {

    /**
     * Creates assignment mixin metadata.
     */
    protected AssignmentMixin() {
    }
}
