package io.sqm.catalog.snapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sqm.catalog.SchemaProvider;
import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;

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
    public CatalogSchema load() throws SQLException {
        try {
            var snapshot = MAPPER.readValue(path.toFile(), SchemaSnapshot.class);
            return toCatalogSchema(snapshot);
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
    public void save(CatalogSchema schema) throws SQLException {
        Objects.requireNonNull(schema, "schema");
        try {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), fromCatalogSchema(schema));
        } catch (IOException e) {
            throw new SQLException("Failed to save schema snapshot to " + path, e);
        }
    }

    private static CatalogSchema toCatalogSchema(SchemaSnapshot snapshot) {
        var tables = new ArrayList<CatalogTable>();
        if (snapshot == null || snapshot.tables == null) {
            return CatalogSchema.of(tables);
        }
        for (var table : snapshot.tables) {
            var columns = new ArrayList<CatalogColumn>();
            if (table.columns != null) {
                for (var column : table.columns) {
                    columns.add(CatalogColumn.of(column.name, column.type));
                }
            }
            tables.add(CatalogTable.of(table.schema, table.name, columns));
        }
        return CatalogSchema.of(tables);
    }

    private static SchemaSnapshot fromCatalogSchema(CatalogSchema schema) {
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
        public CatalogType type;

        /**
         * Creates empty column snapshot.
         */
        public SchemaColumnSnapshot() {
        }

        private SchemaColumnSnapshot(String name, CatalogType type) {
            this.name = name;
            this.type = type;
        }
    }
}

