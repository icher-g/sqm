package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.ExpressionJoin;
import io.sqm.core.Join;
import io.sqm.core.TableJoin;

/**
 * An abstract class used as a placeholder for {@link Join} derived classes mapping.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TableJoin.class, name = "table"),
        @JsonSubTypes.Type(value = ExpressionJoin.class, name = "expr")
})
public abstract class JoinMixIn {
}
