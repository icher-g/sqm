package io.sqm.catalog.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CatalogTableTest {
    @Test
    void column_lookup_is_case_insensitive() {
        var table = CatalogTable.of("public", "users",
            CatalogColumn.of("ID", CatalogType.LONG),
            CatalogColumn.of("Name", CatalogType.STRING)
        );

        assertEquals(CatalogType.LONG, table.column("id").orElseThrow().type());
        assertEquals(CatalogType.STRING, table.column("NAME").orElseThrow().type());
    }

    @Test
    void constructor_rejects_duplicate_columns_case_insensitive() {
        assertThrows(IllegalArgumentException.class, () -> CatalogTable.of("public", "users",
            CatalogColumn.of("ID", CatalogType.LONG),
            CatalogColumn.of("id", CatalogType.INTEGER)
        ));
    }

    @Test
    void table_keeps_primary_and_foreign_key_metadata() {
        var fk = CatalogForeignKey.of("fk_orders_users", List.of("user_id"), "public", "users", List.of("id"));
        var table = CatalogTable.of(
            "public",
            "orders",
            List.of(CatalogColumn.of("id", CatalogType.LONG), CatalogColumn.of("user_id", CatalogType.LONG)),
            List.of("id"),
            List.of(fk)
        );

        assertEquals(List.of("id"), table.primaryKeyColumns());
        assertEquals(1, table.foreignKeys().size());
        assertEquals("fk_orders_users", table.foreignKeys().getFirst().name());
    }
}

