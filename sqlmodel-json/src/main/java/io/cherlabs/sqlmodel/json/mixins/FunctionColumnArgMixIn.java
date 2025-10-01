package io.cherlabs.sqlmodel.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.cherlabs.sqlmodel.core.FunctionColumn;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "kind"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = FunctionColumn.Arg.Column.class, name = "column"),
        @JsonSubTypes.Type(value = FunctionColumn.Arg.Literal.class, name = "literal"),
        @JsonSubTypes.Type(value = FunctionColumn.Arg.Star.class, name = "star"),
        @JsonSubTypes.Type(value = FunctionColumn.Arg.Function.class, name = "func"),
})
public abstract class FunctionColumnArgMixIn {
}
