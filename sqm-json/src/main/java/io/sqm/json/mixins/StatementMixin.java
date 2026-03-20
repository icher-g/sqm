package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.CompositeQuery;
import io.sqm.core.DeleteStatement;
import io.sqm.core.InsertStatement;
import io.sqm.core.MergeStatement;
import io.sqm.core.SelectQuery;
import io.sqm.core.UpdateStatement;
import io.sqm.core.WithQuery;

/**
 * Jackson mixin root for statement polymorphism.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CompositeQuery.Impl.class, name = "composite"),
    @JsonSubTypes.Type(value = SelectQuery.Impl.class, name = "select"),
    @JsonSubTypes.Type(value = WithQuery.Impl.class, name = "with"),
    @JsonSubTypes.Type(value = InsertStatement.Impl.class, name = "insert"),
    @JsonSubTypes.Type(value = UpdateStatement.Impl.class, name = "update"),
    @JsonSubTypes.Type(value = DeleteStatement.Impl.class, name = "delete"),
    @JsonSubTypes.Type(value = MergeStatement.Impl.class, name = "merge")
})
public abstract class StatementMixin extends CommonJsonMixin {

    /**
     * Creates statement mixin metadata.
     */
    protected StatementMixin() {
    }
}
