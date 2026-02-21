package io.sqm.core.transform;

import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.Table;
import io.sqm.core.WithQuery;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.inner;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.with;
import static org.junit.jupiter.api.Assertions.*;

class SchemaQualificationTransformerTest {

    @Test
    void qualifies_unqualified_base_and_join_tables() {
        SelectQuery query = select(col("u", "id"))
            .from(tbl("users").as("u"))
            .join(inner(tbl("orders").as("o")).on(col("u", "id").eq(col("o", "user_id"))));

        var transformer = SchemaQualificationTransformer.of(tableName -> switch (tableName) {
            case "users" -> TableQualification.qualified("app");
            case "orders" -> TableQualification.qualified("sales");
            default -> TableQualification.unresolved();
        });

        var transformed = (SelectQuery) transformer.apply(query);

        var fromTable = (Table) transformed.from();
        assertEquals("app", fromTable.schema());
        assertEquals("users", fromTable.name());
        assertEquals("u", fromTable.alias());

        var joinRight = (Table) transformed.joins().getFirst().right();
        assertEquals("sales", joinRight.schema());
        assertEquals("orders", joinRight.name());
        assertEquals("o", joinRight.alias());
    }

    @Test
    void keeps_already_qualified_table_unchanged() {
        SelectQuery query = select(col("u", "id")).from(tbl("app", "users").as("u"));
        var transformer = SchemaQualificationTransformer.of(tableName -> TableQualification.qualified("ignored"));

        var transformed = (SelectQuery) transformer.apply(query);

        assertSame(query, transformed);
        var fromTable = (Table) transformed.from();
        assertEquals("app", fromTable.schema());
    }

    @Test
    void leaves_cte_reference_unqualified() {
        Query query = with(Query.cte("users", select(col("id")).from(tbl("raw_users"))))
            .body(select(col("id")).from(tbl("users")));

        var resolver = SchemaQualificationTransformer.of(tableName -> {
            var mapping = Map.of(
                "raw_users", "app",
                "users", "app"
            );
            var schema = mapping.get(tableName);
            if (schema == null) {
                return TableQualification.unresolved();
            }
            return TableQualification.qualified(schema);
        });

        var transformed = (WithQuery) resolver.apply(query);

        var cteBody = (SelectQuery) transformed.ctes().getFirst().body();
        assertEquals("app", ((Table) cteBody.from()).schema());

        var outerBody = (SelectQuery) transformed.body();
        assertNull(((Table) outerBody.from()).schema());
        assertEquals("users", ((Table) outerBody.from()).name());
    }

    @Test
    void throws_on_ambiguous_table_resolution() {
        SelectQuery query = select(col("id")).from(tbl("users"));
        var transformer = SchemaQualificationTransformer.of(tableName -> TableQualification.ambiguous());

        assertThrows(
            SchemaQualificationTransformer.AmbiguousTableQualificationException.class,
            () -> transformer.apply(query)
        );
    }
}
