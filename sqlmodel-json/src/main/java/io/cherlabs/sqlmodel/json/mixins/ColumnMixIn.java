package io.cherlabs.sqlmodel.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.cherlabs.sqlmodel.core.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NamedColumn.class,    name = "named"),
        @JsonSubTypes.Type(value = QueryColumn.class,    name = "query"),
        @JsonSubTypes.Type(value = ExpressionColumn.class,name = "expr"),
        @JsonSubTypes.Type(value = FunctionColumn.class,   name = "func"),
        @JsonSubTypes.Type(value = CaseColumn.class, name = "case")
})
public abstract class ColumnMixIn {}
