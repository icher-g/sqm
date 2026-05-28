package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.sqm.core.JsonTableBehavior;

/**
 * Jackson mixin root for table-behavior polymorphism.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@JsonDeserialize(as = JsonTableBehavior.Impl.class)
public abstract class JsonTableBehaviorMixin {

    /**
     * Default constructor.
     */
    public JsonTableBehaviorMixin() {

    }
}
