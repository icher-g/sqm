package io.sqm.json.mixins;

/* =========================
 * ValueSet / row expressions
 * ========================= */

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.QueryExpr;
import io.sqm.core.RowExpr;
import io.sqm.core.RowListExpr;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = RowExpr.Impl.class, name = "row"),
    @JsonSubTypes.Type(value = QueryExpr.Impl.class, name = "query_expr"),
    @JsonSubTypes.Type(value = RowListExpr.Impl.class, name = "row_list")
})
public abstract class ValueSetMixin extends CommonJsonMixin {
}
