package io.cherlabs.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.cherlabs.sqm.core.Values;

/**
 * An abstract class used as a placeholder for {@link Values} derived classes mapping.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Values.Single.class, name = "single"),
        @JsonSubTypes.Type(value = Values.ListValues.class, name = "list"),
        @JsonSubTypes.Type(value = Values.Tuples.class, name = "tuples"),
        @JsonSubTypes.Type(value = Values.Range.class, name = "range"),
        @JsonSubTypes.Type(value = Values.Subquery.class, name = "subquery"),
        @JsonSubTypes.Type(value = Values.Column.class, name = "column")
})
public abstract class ValuesMixIn {
}