package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.sqm.core.NamedTable;
import io.sqm.core.QueryTable;
import io.sqm.core.Table;

/**
 * An abstract class used as a placeholder for {@link Table} derived classes mapping.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NamedTable.class, name = "named"),
        @JsonSubTypes.Type(value = QueryTable.class, name = "query")
})
public abstract class TableMixIn {
}
