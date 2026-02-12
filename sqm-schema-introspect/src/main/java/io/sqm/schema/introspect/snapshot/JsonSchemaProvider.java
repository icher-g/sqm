package io.sqm.schema.introspect.snapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sqm.schema.introspect.SchemaProvider;
import io.sqm.validate.schema.model.DbColumn;
import io.sqm.validate.schema.model.DbSchema;
import io.sqm.validate.schema.model.DbTable;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * File-based schema provider backed by JSON snapshots.
 */
public final class JsonSchemaProvider implements SchemaProvider {
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private final Path path;

    private JsonSchemaProvider(Path path) {
        this.path = Objects.requireNonNull(path, "path");
    }

    /**
     * Creates JSON schema provider for a snapshot file.
     *
     * @param path snapshot file path.
     * @return JSON schema provider.
     */
    public static JsonSchemaProvider of(Path path) {
        return new JsonSchemaProvider(path);
    }

    /**
     * Loads schema metadata from JSON snapshot.
     *
     * @return loaded schema model.
     * @throws SQLException if JSON cannot be read or parsed.
     */
    @Override
    public DbSchema load() throws SQLException {
        try {
            var snapshot = MAPPER.readValue(path.toFile(), SchemaSnapshot.class);
            return toDbSchema(snapshot);
        } catch (IOException e) {
            throw new SQLException("Failed to load schema snapshot from " + path, e);
        }
    }

    /**
     * Saves schema metadata to JSON snapshot.
     *
     * @param schema schema metadata to persist.
     * @throws SQLException if JSON cannot be written.
     */
    public void save(DbSchema schema) throws SQLException {
        Objects.requireNonNull(schema, "schema");
        try {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), fromDbSchema(schema));
        } catch (IOException e) {
            throw new SQLException("Failed to save schema snapshot to " + path, e);
        }
    }

    private static DbSchema toDbSchema(SchemaSnapshot snapshot) {
        var tables = new ArrayList<DbTable>();
        if (snapshot == null || snapshot.tables == null) {
            return DbSchema.of(tables);
        }
        for (var table : snapshot.tables) {
            var columns = new ArrayList<DbColumn>();
            if (table.columns != null) {
                for (var column : table.columns) {
                    columns.add(DbColumn.of(column.name, column.type));
                }
            }
            tables.add(DbTable.of(table.schema, table.name, columns));
        }
        return DbSchema.of(tables);
    }

    private static SchemaSnapshot fromDbSchema(DbSchema schema) {
        var tables = new ArrayList<SchemaTableSnapshot>();
        for (var table : schema.tables()) {
            var columns = new ArrayList<SchemaColumnSnapshot>();
            for (var column : table.columns()) {
                columns.add(new SchemaColumnSnapshot(column.name(), column.type()));
            }
            tables.add(new SchemaTableSnapshot(table.schema(), table.name(), columns));
        }
        return new SchemaSnapshot(tables);
    }

    /**
     * Schema snapshot document root.
     */
    public static final class SchemaSnapshot {
        /**
         * Persisted table list.
         */
        public List<SchemaTableSnapshot> tables;

        /**
         * Creates empty snapshot.
         */
        public SchemaSnapshot() {
        }

        private SchemaSnapshot(List<SchemaTableSnapshot> tables) {
            this.tables = tables;
        }
    }

    /**
     * Persisted table snapshot.
     */
    public static final class SchemaTableSnapshot {
        /**
         * Table schema.
         */
        public String schema;
        /**
         * Table name.
         */
        public String name;
        /**
         * Persisted columns.
         */
        public List<SchemaColumnSnapshot> columns;

        /**
         * Creates empty table snapshot.
         */
        public SchemaTableSnapshot() {
        }

        private SchemaTableSnapshot(String schema, String name, List<SchemaColumnSnapshot> columns) {
            this.schema = schema;
            this.name = name;
            this.columns = columns;
        }
    }

    /**
     * Persisted column snapshot.
     */
    public static final class SchemaColumnSnapshot {
        /**
         * Column name.
         */
        public String name;
        /**
         * Column semantic type.
         */
        public io.sqm.validate.schema.model.DbType type;

        /**
         * Creates empty column snapshot.
         */
        public SchemaColumnSnapshot() {
        }

        private SchemaColumnSnapshot(String name, io.sqm.validate.schema.model.DbType type) {
            this.name = name;
            this.type = type;
        }
    }
}

