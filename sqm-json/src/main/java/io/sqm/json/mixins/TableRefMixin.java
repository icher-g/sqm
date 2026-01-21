package io.sqm.json.mixins;

/* ===============
 * Table reference
 * =============== */

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.QueryTable;
import io.sqm.core.Table;
import io.sqm.core.ValuesTable;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = QueryTable.Impl.class, name = "query_table"),
    @JsonSubTypes.Type(value = Table.Impl.class, name = "table"),
    @JsonSubTypes.Type(value = ValuesTable.Impl.class, name = "values")
})
public abstract class TableRefMixin extends CommonJsonMixin {
}
