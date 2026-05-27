package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.JsonTableExistsColumn;
import io.sqm.core.JsonTableNestedPathColumn;
import io.sqm.core.JsonTableOrdinalityColumn;
import io.sqm.core.JsonTableScalarColumn;

/**
 * Jackson mixin root for JSON table column polymorphism.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = JsonTableScalarColumn.Impl.class, name = "scalar"),
    @JsonSubTypes.Type(value = JsonTableOrdinalityColumn.Impl.class, name = "ordinality"),
    @JsonSubTypes.Type(value = JsonTableExistsColumn.Impl.class, name = "exists"),
    @JsonSubTypes.Type(value = JsonTableNestedPathColumn.Impl.class, name = "nested_path")
})
public abstract class JsonTableColumnMixin extends CommonJsonMixin {
    /**
     * Creates JSON table column mixin metadata.
     */
    protected JsonTableColumnMixin() {
    }
}
