package io.sqm.json.mixins;

/* ===============
 * Table reference
 * =============== */

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.internal.QueryTableImpl;
import io.sqm.core.internal.TableImpl;
import io.sqm.core.internal.ValuesTableImpl;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = QueryTableImpl.class, name = "query_table"),
    @JsonSubTypes.Type(value = TableImpl.class, name = "table"),
    @JsonSubTypes.Type(value = ValuesTableImpl.class, name = "values")
})
public abstract class TableRefMixin extends CommonJsonMixin {
}
