package io.sqm.core.transform;

import io.sqm.core.Query;
import io.sqm.core.Identifier;
import io.sqm.core.QuoteStyle;
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
            .join(inner(tbl("orders").as("o")).on(col("u", "id").eq(col("o", "user_id"))))
            .build();

        var transformer = SchemaQualificationTransformer.of(tableName -> switch (tableName) {
            case "users" -> TableQualification.qualified(Identifier.of("app"));
            case "orders" -> TableQualification.qualified(Identifier.of("sales"));
            default -> TableQualification.unresolved();
        });

        var transformed = (SelectQuery) transformer.apply(query);

        var fromTable = (Table) transformed.from();
        assertEquals("app", fromTable.schema().value());
        assertEquals("users", fromTable.name().value());
        assertEquals("u", fromTable.alias().value());

        var joinRight = (Table) transformed.joins().getFirst().right();
        assertEquals("sales", joinRight.schema().value());
        assertEquals("orders", joinRight.name().value());
        assertEquals("o", joinRight.alias().value());
    }

    @Test
    void keeps_already_qualified_table_unchanged() {
        SelectQuery query = select(col("u", "id")).from(tbl("app", "users").as("u")).build();
        var transformer = SchemaQualificationTransformer.of(tableName -> TableQualification.qualified(Identifier.of("ignored")));

        var transformed = (SelectQuery) transformer.apply(query);

        assertSame(query, transformed);
        var fromTable = (Table) transformed.from();
        assertEquals("app", fromTable.schema().value());
    }

    @Test
    void leaves_cte_reference_unqualified() {
        Query query = with(Query.cte(io.sqm.core.Identifier.of("users"), select(col("id")).from(tbl("raw_users")).build()))
            .body(select(col("id")).from(tbl("users")).build());

        var resolver = SchemaQualificationTransformer.of(tableName -> {
            var mapping = Map.of(
                "raw_users", "app",
                "users", "app"
            );
            var schema = mapping.get(tableName);
            if (schema == null) {
                return TableQualification.unresolved();
            }
            return TableQualification.qualified(Identifier.of(schema));
        });

        var transformed = (WithQuery) resolver.apply(query);

        var cteBody = (SelectQuery) transformed.ctes().getFirst().body();
        assertEquals("app", ((Table) cteBody.from()).schema().value());

        var outerBody = (SelectQuery) transformed.body();
        assertNull(((Table) outerBody.from()).schema());
        assertEquals("users", ((Table) outerBody.from()).name().value());
    }

    @Test
    void throws_on_ambiguous_table_resolution() {
        SelectQuery query = select(col("id")).from(tbl("users")).build();
        var transformer = SchemaQualificationTransformer.of(tableName -> TableQualification.ambiguous());

        assertThrows(
            SchemaQualificationTransformer.AmbiguousTableQualificationException.class,
            () -> transformer.apply(query)
        );
    }

    @Test
    void preserves_quote_metadata_on_injected_schema_identifier() {
        SelectQuery query = select(col("id")).from(tbl("users")).build();
        var transformer = SchemaQualificationTransformer.of(tableName ->
            TableQualification.qualified(io.sqm.core.Identifier.of("App", QuoteStyle.DOUBLE_QUOTE))
        );

        var transformed = (SelectQuery) transformer.apply(query);
        var fromTable = (Table) transformed.from();

        assertEquals("App", fromTable.schema().value());
        assertNotNull(fromTable.schema());
        assertEquals(QuoteStyle.DOUBLE_QUOTE, fromTable.schema().quoteStyle());
    }
}
