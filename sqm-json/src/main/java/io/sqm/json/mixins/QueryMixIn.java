package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.*;

/**
 * An abstract class used as a placeholder for {@link Query} derived classes mapping.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SelectQuery.class, name = "select"),
    @JsonSubTypes.Type(value = WithQuery.class, name = "with"),
    @JsonSubTypes.Type(value = CompositeQuery.class, name = "composite"),
    @JsonSubTypes.Type(value = CteQuery.class, name = "cte")
})
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
public abstract class QueryMixIn {
}
