package io.sqm.core.transform;

import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.Table;
import io.sqm.core.WithQuery;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.with;
import static org.junit.jupiter.api.Assertions.*;

class SchemaQualificationTransformerAdditionalTest {

    @Test
    void factory_rejects_null_resolver() {
        assertThrows(NullPointerException.class, () -> SchemaQualificationTransformer.of(null));
    }

    @Test
    void unresolved_table_stays_unchanged() {
        SelectQuery query = select(col("id")).from(tbl("users")).build();
        var transformer = SchemaQualificationTransformer.of(t -> TableQualification.unresolved());

        var transformed = (SelectQuery) transformer.apply(query);

        assertSame(query, transformed);
    }

    @Test
    void recursive_cte_reference_in_body_is_not_qualified() {
        Query query = with(Query.cte("chain", select(col("id")).from(tbl("chain")).build()))
            .recursive(true)
            .body(select(col("id")).from(tbl("chain")).build());

        var transformer = SchemaQualificationTransformer.of(t -> TableQualification.qualified("app"));
        var transformed = (WithQuery) transformer.apply(query);
        var body = (SelectQuery) transformed.body();

        assertNull(((Table) body.from()).schema());
        assertEquals("chain", ((Table) body.from()).name());
    }
}
