package io.cherlabs.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.cherlabs.sqm.core.*;

/**
 * An abstract class used as a placeholder for {@link Column} derived classes mapping.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NamedColumn.class,    name = "named"),
        @JsonSubTypes.Type(value = QueryColumn.class,    name = "query"),
        @JsonSubTypes.Type(value = ExpressionColumn.class,name = "expr"),
        @JsonSubTypes.Type(value = FunctionColumn.class,   name = "func"),
        @JsonSubTypes.Type(value = CaseColumn.class, name = "case")
})
public abstract class ColumnMixIn {}
