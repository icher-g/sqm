package io.sqm.validate.schema.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DbSchemaTest {

    @Test
    void resolve_findsTableByExplicitSchema_caseInsensitive() {
        var schema = DbSchema.of(
            DbTable.of("public", "users", DbColumn.of("id", DbType.LONG))
        );

        var result = schema.resolve("PUBLIC", "USERS");
        assertTrue(result.ok());
        assertInstanceOf(DbSchema.TableLookupResult.Found.class, result);
    }

    @Test
    void resolve_returnsNotFound_whenMissing() {
        var schema = DbSchema.of(
            DbTable.of("public", "users", DbColumn.of("id", DbType.LONG))
        );

        var result = schema.resolve(null, "orders");
        assertFalse(result.ok());
        assertInstanceOf(DbSchema.TableLookupResult.NotFound.class, result);
    }

    @Test
    void resolve_returnsAmbiguous_forUnqualifiedNameAcrossSchemas() {
        var schema = DbSchema.of(
            DbTable.of("public", "users", DbColumn.of("id", DbType.LONG)),
            DbTable.of("audit", "users", DbColumn.of("id", DbType.LONG))
        );

        var result = schema.resolve(null, "users");
        assertFalse(result.ok());
        var ambiguous = assertInstanceOf(DbSchema.TableLookupResult.Ambiguous.class, result);
        assertEquals("users", ambiguous.name());
        assertEquals(2, ambiguous.matches().size());
    }

    @Test
    void of_rejectsDuplicateSchemaAndTableNames_caseInsensitive() {
        var ex = assertThrows(IllegalArgumentException.class, () -> DbSchema.of(
            DbTable.of("public", "users", DbColumn.of("id", DbType.LONG)),
            DbTable.of("PUBLIC", "USERS", DbColumn.of("id", DbType.LONG))
        ));
        assertTrue(ex.getMessage().contains("Duplicate table"));
    }
}
