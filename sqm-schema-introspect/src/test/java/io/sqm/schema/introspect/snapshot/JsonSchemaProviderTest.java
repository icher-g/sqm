package io.sqm.schema.introspect.snapshot;

import io.sqm.validate.schema.model.DbColumn;
import io.sqm.validate.schema.model.DbSchema;
import io.sqm.validate.schema.model.DbTable;
import io.sqm.validate.schema.model.DbType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSchemaProviderTest {
    @TempDir
    Path tempDir;

    @Test
    void saveAndLoad_roundTripsSchemaSnapshot() throws Exception {
        var schema = DbSchema.of(
            DbTable.of("public", "users",
                DbColumn.of("id", DbType.LONG),
                DbColumn.of("name", DbType.STRING),
                DbColumn.of("payload", DbType.JSONB)
            )
        );
        var file = tempDir.resolve("schema.json");
        var provider = JsonSchemaProvider.of(file);

        provider.save(schema);
        var loaded = provider.load();

        var users = ((DbSchema.TableLookupResult.Found) loaded.resolve("public", "users")).table();
        assertEquals(DbType.LONG, users.column("id").orElseThrow().type());
        assertEquals(DbType.STRING, users.column("name").orElseThrow().type());
        assertEquals(DbType.JSONB, users.column("payload").orElseThrow().type());
        assertTrue(Files.size(file) > 0);
    }

    @Test
    void load_throwsSqlExceptionForInvalidJson() throws Exception {
        var file = tempDir.resolve("broken-schema.json");
        Files.writeString(file, "{not valid json");
        var provider = JsonSchemaProvider.of(file);

        var ex = assertThrows(SQLException.class, provider::load);

        assertTrue(ex.getMessage().contains("Failed to load schema snapshot"));
    }
}

