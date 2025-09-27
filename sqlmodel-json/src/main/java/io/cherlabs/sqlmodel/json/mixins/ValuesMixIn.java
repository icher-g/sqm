package io.cherlabs.sqlmodel.json.mixins;

import com.fasterxml.jackson.annotation.*;
import io.cherlabs.sqlmodel.core.Values;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Values.Single.class, name = "single"),
        @JsonSubTypes.Type(value = Values.ListValues.class, name = "list"),
        @JsonSubTypes.Type(value = Values.Tuples.class,  name = "tuples"),
        @JsonSubTypes.Type(value = Values.Range.class,   name = "range"),
        @JsonSubTypes.Type(value = Values.Subquery.class,name = "subquery")
})
public abstract class ValuesMixIn {}