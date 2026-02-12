package io.sqm.schema.introspect.jdbc;

import io.sqm.schema.introspect.SchemaProvider;
import io.sqm.schema.introspect.postgresql.PostgresSqlTypeMapper;
import io.sqm.validate.schema.model.DbColumn;
import io.sqm.validate.schema.model.DbSchema;
import io.sqm.validate.schema.model.DbTable;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * JDBC-based schema provider that builds {@link DbSchema} from metadata.
 *
 * <p>This implementation is tuned for PostgreSQL usage by default through
 * {@link PostgresSqlTypeMapper} and can be customized with any {@link SqlTypeMapper}.</p>
 */
public final class JdbcSchemaProvider implements SchemaProvider {
    private static final List<String> DEFAULT_TABLE_TYPES = List.of(
        "TABLE",
        "VIEW",
        "MATERIALIZED VIEW",
        "FOREIGN TABLE"
    );

    private final DataSource dataSource;
    private final String catalog;
    private final String schemaPattern;
    private final List<String> tableTypes;
    private final SqlTypeMapper typeMapper;

    private JdbcSchemaProvider(
        DataSource dataSource,
        String catalog,
        String schemaPattern,
        Collection<String> tableTypes,
        SqlTypeMapper typeMapper
    ) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.catalog = catalog;
        this.schemaPattern = schemaPattern;
        this.tableTypes = normalizeTableTypes(tableTypes);
        this.typeMapper = Objects.requireNonNull(typeMapper, "typeMapper");
    }

    /**
     * Creates provider with default PostgreSQL-oriented settings.
     *
     * @param dataSource JDBC data source.
     * @return JDBC schema provider.
     */
    public static JdbcSchemaProvider of(DataSource dataSource) {
        return new JdbcSchemaProvider(
            dataSource,
            null,
            null,
            DEFAULT_TABLE_TYPES,
            PostgresSqlTypeMapper.standard()
        );
    }

    /**
     * Creates provider scoped by schema pattern.
     *
     * @param dataSource JDBC data source.
     * @param schemaPattern schema name pattern for table introspection.
     * @return JDBC schema provider.
     */
    public static JdbcSchemaProvider of(DataSource dataSource, String schemaPattern) {
        return new JdbcSchemaProvider(
            dataSource,
            null,
            schemaPattern,
            DEFAULT_TABLE_TYPES,
            PostgresSqlTypeMapper.standard()
        );
    }

    /**
     * Creates fully customized JDBC schema provider.
     *
     * @param dataSource JDBC data source.
     * @param catalog catalog filter.
     * @param schemaPattern schema filter.
     * @param tableTypes table types to introspect.
     * @param typeMapper SQL type mapper.
     * @return JDBC schema provider.
     */
    public static JdbcSchemaProvider of(
        DataSource dataSource,
        String catalog,
        String schemaPattern,
        Collection<String> tableTypes,
        SqlTypeMapper typeMapper
    ) {
        return new JdbcSchemaProvider(dataSource, catalog, schemaPattern, tableTypes, typeMapper);
    }

    /**
     * Loads schema metadata from JDBC database metadata.
     *
     * @return database schema model.
     * @throws SQLException if metadata cannot be read.
     */
    @Override
    public DbSchema load() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            var metadata = connection.getMetaData();
            var tables = loadTables(metadata);
            return DbSchema.of(tables);
        }
    }

    private List<DbTable> loadTables(DatabaseMetaData metadata) throws SQLException {
        var tables = new ArrayList<DbTable>();
        try (var result = metadata.getTables(catalog, schemaPattern, "%", tableTypesArray())) {
            while (result.next()) {
                var tableName = result.getString("TABLE_NAME");
                if (tableName == null || tableName.isBlank()) {
                    continue;
                }
                var tableSchema = result.getString("TABLE_SCHEM");
                var columns = loadColumns(metadata, tableSchema, tableName);
                tables.add(DbTable.of(blankAsNull(tableSchema), tableName, columns));
            }
        }
        return tables;
    }

    private List<DbColumn> loadColumns(DatabaseMetaData metadata, String tableSchema, String tableName) throws SQLException {
        var columns = new ArrayList<ColumnDef>();
        try (ResultSet result = metadata.getColumns(catalog, tableSchema, tableName, "%")) {
            while (result.next()) {
                var columnName = result.getString("COLUMN_NAME");
                if (columnName == null || columnName.isBlank()) {
                    continue;
                }
                var jdbcType = result.getInt("DATA_TYPE");
                if (result.wasNull()) {
                    jdbcType = Types.OTHER;
                }
                var nativeTypeName = result.getString("TYPE_NAME");
                var ordinal = result.getInt("ORDINAL_POSITION");
                if (result.wasNull()) {
                    ordinal = Integer.MAX_VALUE;
                }
                var type = typeMapper.map(nativeTypeName, jdbcType);
                columns.add(new ColumnDef(ordinal, DbColumn.of(columnName, type)));
            }
        }
        columns.sort(Comparator.comparingInt(ColumnDef::ordinal));
        return columns.stream().map(ColumnDef::column).toList();
    }

    private String[] tableTypesArray() {
        return tableTypes.toArray(String[]::new);
    }

    private static String blankAsNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static List<String> normalizeTableTypes(Collection<String> tableTypes) {
        Objects.requireNonNull(tableTypes, "tableTypes");
        if (tableTypes.isEmpty()) {
            throw new IllegalArgumentException("tableTypes cannot be empty");
        }
        var unique = new LinkedHashSet<String>();
        for (var tableType : tableTypes) {
            if (tableType == null || tableType.isBlank()) {
                continue;
            }
            unique.add(tableType);
        }
        if (unique.isEmpty()) {
            throw new IllegalArgumentException("tableTypes cannot be blank");
        }
        return List.copyOf(unique);
    }

    private record ColumnDef(int ordinal, DbColumn column) {
    }
}

