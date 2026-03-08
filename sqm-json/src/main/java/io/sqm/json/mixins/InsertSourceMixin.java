package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.CompositeQuery;
import io.sqm.core.RowExpr;
import io.sqm.core.RowListExpr;
import io.sqm.core.SelectQuery;
import io.sqm.core.WithQuery;

/**
 * Jackson mixin root for insert-source polymorphism.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SelectQuery.Impl.class, name = "select"),
    @JsonSubTypes.Type(value = CompositeQuery.Impl.class, name = "composite"),
    @JsonSubTypes.Type(value = WithQuery.Impl.class, name = "with"),
    @JsonSubTypes.Type(value = RowExpr.Impl.class, name = "row"),
    @JsonSubTypes.Type(value = RowListExpr.Impl.class, name = "row_list")
})
public abstract class InsertSourceMixin extends CommonJsonMixin {

    /**
     * Creates insert-source mixin metadata.
     */
    protected InsertSourceMixin() {
    }
}