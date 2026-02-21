package io.sqm.catalog.jdbc;

import io.sqm.catalog.SchemaProvider;
import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogForeignKey;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * JDBC-based schema provider that builds {@link CatalogSchema} from metadata.
 *
 * <p>This implementation uses {@link DefaultSqlTypeMapper} by default and also
 * supports explicit {@link SqlTypeMapper} configuration for dialect-specific mapping.</p>
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
     * Creates provider with default JDBC metadata settings.
     *
     * <p>This overload uses {@link DefaultSqlTypeMapper#standard()}.</p>
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
            DefaultSqlTypeMapper.standard()
        );
    }

    /**
     * Creates provider with default JDBC metadata settings.
     *
     * <p>This overload accepts explicit SQL type mapper configuration.</p>
     *
     * @param dataSource JDBC data source.
     * @param typeMapper SQL type mapper.
     * @return JDBC schema provider.
     */
    public static JdbcSchemaProvider of(DataSource dataSource, SqlTypeMapper typeMapper) {
        return new JdbcSchemaProvider(
            dataSource,
            null,
            null,
            DEFAULT_TABLE_TYPES,
            typeMapper
        );
    }

    /**
     * Creates provider scoped by schema pattern.
     *
     * <p>This overload uses {@link DefaultSqlTypeMapper#standard()}.</p>
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
            DefaultSqlTypeMapper.standard()
        );
    }

    /**
     * Creates provider scoped by schema pattern.
     *
     * @param dataSource JDBC data source.
     * @param schemaPattern schema name pattern for table introspection.
     * @param typeMapper SQL type mapper.
     * @return JDBC schema provider.
     */
    public static JdbcSchemaProvider of(DataSource dataSource, String schemaPattern, SqlTypeMapper typeMapper) {
        return new JdbcSchemaProvider(
            dataSource,
            null,
            schemaPattern,
            DEFAULT_TABLE_TYPES,
            typeMapper
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
    public CatalogSchema load() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            var metadata = connection.getMetaData();
            var tables = loadTables(metadata);
            return CatalogSchema.of(tables);
        }
    }

    private List<CatalogTable> loadTables(DatabaseMetaData metadata) throws SQLException {
        var tables = new ArrayList<CatalogTable>();
        try (var result = metadata.getTables(catalog, schemaPattern, "%", tableTypesArray())) {
            while (result.next()) {
                var tableName = result.getString("TABLE_NAME");
                if (tableName == null || tableName.isBlank()) {
                    continue;
                }
                var tableSchema = result.getString("TABLE_SCHEM");
                var columns = loadColumns(metadata, tableSchema, tableName);
                var primaryKeyColumns = loadPrimaryKeyColumns(metadata, tableSchema, tableName);
                var foreignKeys = loadForeignKeys(metadata, tableSchema, tableName);
                tables.add(CatalogTable.of(blankAsNull(tableSchema), tableName, columns, primaryKeyColumns, foreignKeys));
            }
        }
        return tables;
    }

    private List<CatalogColumn> loadColumns(DatabaseMetaData metadata, String tableSchema, String tableName) throws SQLException {
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
                columns.add(new ColumnDef(ordinal, CatalogColumn.of(columnName, type)));
            }
        }
        columns.sort(Comparator.comparingInt(ColumnDef::ordinal));
        return columns.stream().map(ColumnDef::column).toList();
    }

    private String[] tableTypesArray() {
        return tableTypes.toArray(String[]::new);
    }

    private List<String> loadPrimaryKeyColumns(DatabaseMetaData metadata, String tableSchema, String tableName) throws SQLException {
        var columns = new ArrayList<KeyColumn>();
        try (var result = metadata.getPrimaryKeys(catalog, tableSchema, tableName)) {
            while (result.next()) {
                var columnName = result.getString("COLUMN_NAME");
                if (columnName == null || columnName.isBlank()) {
                    continue;
                }
                var keySeq = result.getInt("KEY_SEQ");
                if (result.wasNull()) {
                    keySeq = Integer.MAX_VALUE;
                }
                columns.add(new KeyColumn(keySeq, columnName));
            }
        }
        columns.sort(Comparator.comparingInt(KeyColumn::keySeq));
        return columns.stream().map(KeyColumn::column).toList();
    }

    private List<CatalogForeignKey> loadForeignKeys(DatabaseMetaData metadata, String tableSchema, String tableName) throws SQLException {
        var ordered = new LinkedHashMap<String, ForeignKeyDef>();
        var anonymousCounter = new int[]{0};
        try (var result = metadata.getImportedKeys(catalog, tableSchema, tableName)) {
            while (result.next()) {
                var sourceColumn = result.getString("FKCOLUMN_NAME");
                var targetTable = result.getString("PKTABLE_NAME");
                var targetColumn = result.getString("PKCOLUMN_NAME");
                if (sourceColumn == null || sourceColumn.isBlank()
                    || targetTable == null || targetTable.isBlank()
                    || targetColumn == null || targetColumn.isBlank()) {
                    continue;
                }
                var targetSchema = blankAsNull(result.getString("PKTABLE_SCHEM"));
                var fkName = blankAsNull(result.getString("FK_NAME"));
                var keySeq = result.getInt("KEY_SEQ");
                if (result.wasNull()) {
                    keySeq = Integer.MAX_VALUE;
                }
                var groupKey = fkName == null
                    ? "anon:%d".formatted(anonymousCounter[0]++)
                    : "name:%s".formatted(fkName.toLowerCase(Locale.ROOT));
                var def = ordered.computeIfAbsent(groupKey, ignored -> new ForeignKeyDef(
                    fkName,
                    targetSchema,
                    targetTable,
                    new ArrayList<>(),
                    new ArrayList<>()
                ));
                def.sourceColumns().add(new KeyColumn(keySeq, sourceColumn));
                def.targetColumns().add(new KeyColumn(keySeq, targetColumn));
            }
        }
        return ordered.values().stream()
            .map(def -> CatalogForeignKey.of(
                def.name(),
                sortColumns(def.sourceColumns()),
                def.targetSchema(),
                def.targetTable(),
                sortColumns(def.targetColumns())
            ))
            .toList();
    }

    private static List<String> sortColumns(List<KeyColumn> columns) {
        return columns.stream()
            .sorted(Comparator.comparingInt(KeyColumn::keySeq))
            .map(KeyColumn::column)
            .toList();
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

    private record ColumnDef(int ordinal, CatalogColumn column) {
    }

    private record KeyColumn(int keySeq, String column) {
    }

    private record ForeignKeyDef(
        String name,
        String targetSchema,
        String targetTable,
        List<KeyColumn> sourceColumns,
        List<KeyColumn> targetColumns
    ) {
    }
}

