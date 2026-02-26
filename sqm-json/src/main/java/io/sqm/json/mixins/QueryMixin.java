package io.sqm.json.mixins;

/* =========
 * Query AST
 * ========= */

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.CompositeQuery;
import io.sqm.core.SelectQuery;
import io.sqm.core.WithQuery;

/**
 * Jackson mixin root for query polymorphism.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CompositeQuery.Impl.class, name = "composite"),
    @JsonSubTypes.Type(value = SelectQuery.Impl.class, name = "select"),
    @JsonSubTypes.Type(value = WithQuery.Impl.class, name = "with")
})
public abstract class QueryMixin extends CommonJsonMixin {

    /**
     * Creates query mixin metadata.
     */
    protected QueryMixin() {
    }
}
