package io.cherlabs.sqlmodel.json.mixins;

import com.fasterxml.jackson.annotation.*;
import io.cherlabs.sqlmodel.core.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ColumnFilter.class,    name = "column"),
        @JsonSubTypes.Type(value = TupleFilter.class,    name = "tuple"),
        @JsonSubTypes.Type(value = CompositeFilter.class, name = "composite"),
        @JsonSubTypes.Type(value = ExpressionFilter.class,name = "expr")
})
public abstract class FilterMixIn {}
