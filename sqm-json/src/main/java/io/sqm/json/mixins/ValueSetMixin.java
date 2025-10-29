package io.sqm.json.mixins;

/* =========================
 * ValueSet / row expressions
 * ========================= */

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.internal.QueryExprImpl;
import io.sqm.core.internal.RowExprImpl;
import io.sqm.core.internal.RowListExprImpl;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = RowExprImpl.class, name = "row"),
    @JsonSubTypes.Type(value = QueryExprImpl.class, name = "query_expr"),
    @JsonSubTypes.Type(value = RowListExprImpl.class, name = "row_list")
})
public abstract class ValueSetMixin extends CommonJsonMixin {
}
