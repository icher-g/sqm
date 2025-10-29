package io.sqm.json.mixins;

/* =========
 * Query AST
 * ========= */

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.internal.CompositeQueryImpl;
import io.sqm.core.internal.SelectQueryImpl;
import io.sqm.core.internal.WithQueryImpl;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CompositeQueryImpl.class, name = "composite"),
    @JsonSubTypes.Type(value = SelectQueryImpl.class, name = "select"),
    @JsonSubTypes.Type(value = WithQueryImpl.class, name = "with")
})
public abstract class QueryMixin extends CommonJsonMixin {
}
