package io.cherlabs.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.cherlabs.sqm.core.NamedTable;
import io.cherlabs.sqm.core.QueryTable;
import io.cherlabs.sqm.core.Table;

/**
 * An abstract class used as a placeholder for {@link Table} derived classes mapping.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NamedTable.class, name = "table"),
        @JsonSubTypes.Type(value = QueryTable.class, name = "query")
})
public abstract class TableMixIn {
}
