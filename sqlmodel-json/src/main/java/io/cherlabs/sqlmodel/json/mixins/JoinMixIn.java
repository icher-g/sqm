package io.cherlabs.sqlmodel.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.cherlabs.sqlmodel.core.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TableJoin.class,    name = "table"),
        @JsonSubTypes.Type(value = ExpressionJoin.class,name = "expr")
})
public class JoinMixIn {}
