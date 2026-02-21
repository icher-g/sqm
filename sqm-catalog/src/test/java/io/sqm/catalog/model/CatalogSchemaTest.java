package io.sqm.catalog.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CatalogSchemaTest {
    @Test
    void resolve_supports_qualified_and_unqualified_lookups() {
        var users = CatalogTable.of("public", "users", CatalogColumn.of("id", CatalogType.LONG));
        var schema = CatalogSchema.of(users);

        var qualified = schema.resolve("public", "users");
        var unqualified = schema.resolve(null, "users");

        assertTrue(qualified.ok());
        assertTrue(unqualified.ok());
    }

    @Test
    void resolve_reports_ambiguous_when_multiple_tables_share_name() {
        var s = CatalogSchema.of(
            CatalogTable.of("public", "users", CatalogColumn.of("id", CatalogType.LONG)),
            CatalogTable.of("tenant_a", "users", CatalogColumn.of("id", CatalogType.LONG))
        );

        var result = s.resolve(null, "users");
        assertInstanceOf(CatalogSchema.TableLookupResult.Ambiguous.class, result);
        assertFalse(result.ok());
        assertEquals(2, ((CatalogSchema.TableLookupResult.Ambiguous) result).matches().size());
    }

    @Test
    void constructor_rejects_duplicate_schema_table_pairs_case_insensitive() {
        assertThrows(IllegalArgumentException.class, () -> CatalogSchema.of(
            CatalogTable.of("PUBLIC", "users", CatalogColumn.of("id", CatalogType.LONG)),
            CatalogTable.of("public", "USERS", CatalogColumn.of("id", CatalogType.LONG))
        ));
    }

    @Test
    void resolve_reports_not_found() {
        var schema = CatalogSchema.of(CatalogTable.of("public", "users", CatalogColumn.of("id", CatalogType.LONG)));
        var result = schema.resolve("public", "missing");
        assertInstanceOf(CatalogSchema.TableLookupResult.NotFound.class, result);
        assertFalse(result.ok());
    }
}

