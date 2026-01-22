package io.sqm.json.mixins;

/* ===============
 * Table reference
 * =============== */

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = QueryTable.Impl.class, name = "query_table"),
    @JsonSubTypes.Type(value = Table.Impl.class, name = "table"),
    @JsonSubTypes.Type(value = ValuesTable.Impl.class, name = "values"),
    @JsonSubTypes.Type(value = FunctionTable.Impl.class, name = "func_table"),
    @JsonSubTypes.Type(value = Lateral.Impl.class, name = "lateral")
})
public abstract class TableRefMixin extends CommonJsonMixin {
}
