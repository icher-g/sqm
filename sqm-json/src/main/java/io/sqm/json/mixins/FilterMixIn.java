package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.*;

/**
 * An abstract class used as a placeholder for {@link Filter} derived classes mapping.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ColumnFilter.class, name = "column"),
    @JsonSubTypes.Type(value = TupleFilter.class, name = "tuple"),
    @JsonSubTypes.Type(value = CompositeFilter.class, name = "composite"),
    @JsonSubTypes.Type(value = ExpressionFilter.class, name = "expr")
})
public abstract class FilterMixIn {
}
