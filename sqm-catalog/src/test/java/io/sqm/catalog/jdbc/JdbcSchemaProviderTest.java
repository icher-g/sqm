package io.sqm.catalog.jdbc;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogType;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class JdbcSchemaProviderTest {
    private static DataSource dataSourceProxy(DatabaseMetaData metadata) throws SQLException {
        Objects.requireNonNull(metadata, "metadata");
        try (var connection = connectionProxy(metadata)) {
            return new DataSource() {
                @Override
                public Connection getConnection() {
                    return connection;
                }

                @Override
                public Connection getConnection(String username, String password) {
                    return connection;
                }

                @Override
                public PrintWriter getLogWriter() {
                    return null;
                }

                @Override
                public void setLogWriter(PrintWriter out) {
                }

                @Override
                public int getLoginTimeout() {
                    return 0;
                }

                @Override
                public void setLoginTimeout(int seconds) {
                }

                @Override
                public Logger getParentLogger() throws SQLFeatureNotSupportedException {
                    throw new SQLFeatureNotSupportedException();
                }

                @Override
                public <T> T unwrap(Class<T> iface) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean isWrapperFor(Class<?> iface) {
                    return false;
                }
            };
        }
    }

    private static Connection connectionProxy(DatabaseMetaData metadata) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getMetaData" -> metadata;
            case "close" -> null;
            case "isClosed", "isWrapperFor" -> false;
            case "unwrap" -> throw new UnsupportedOperationException();
            case "toString" -> "ConnectionProxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new UnsupportedOperationException("Unsupported connection method: " + method.getName());
        };
        return (Connection) Proxy.newProxyInstance(
            JdbcSchemaProviderTest.class.getClassLoader(),
            new Class[]{Connection.class},
            handler
        );
    }

    private static DatabaseMetaData metadataProxy(
        MetadataState state,
        List<Map<String, Object>> tableRows,
        Map<String, List<Map<String, Object>>> columnRowsByTable,
        Map<String, List<Map<String, Object>>> primaryKeyRowsByTable,
        Map<String, List<Map<String, Object>>> importedKeyRowsByTable
    ) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getTables" -> {
                state.tablesCatalog = (String) args[0];
                state.tablesSchemaPattern = (String) args[1];
                state.tablesTypes = (String[]) args[3];
                yield resultSetProxy(tableRows);
            }
            case "getColumns" -> {
                var tableName = (String) args[2];
                var rows = columnRowsByTable.getOrDefault(tableName, List.of());
                state.requestedColumnTables.add(tableName);
                yield resultSetProxy(rows);
            }
            case "getPrimaryKeys" -> {
                var tableName = (String) args[2];
                var rows = primaryKeyRowsByTable.getOrDefault(tableName, List.of());
                state.requestedPrimaryKeyTables.add(tableName);
                yield resultSetProxy(rows);
            }
            case "getImportedKeys" -> {
                var tableName = (String) args[2];
                var rows = importedKeyRowsByTable.getOrDefault(tableName, List.of());
                state.requestedImportedKeyTables.add(tableName);
                yield resultSetProxy(rows);
            }
            case "unwrap" -> throw new UnsupportedOperationException();
            case "isWrapperFor" -> false;
            case "toString" -> "DatabaseMetaDataProxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new UnsupportedOperationException("Unsupported metadata method: " + method.getName());
        };
        return (DatabaseMetaData) Proxy.newProxyInstance(
            JdbcSchemaProviderTest.class.getClassLoader(),
            new Class[]{DatabaseMetaData.class},
            handler
        );
    }

    private static ResultSet resultSetProxy(List<Map<String, Object>> rows) {
        var index = new int[]{-1};
        var wasNull = new boolean[]{false};
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "next" -> {
                index[0]++;
                yield index[0] < rows.size();
            }
            case "getString" -> {
                var value = current(rows, index[0]).get((String) args[0]);
                wasNull[0] = value == null;
                yield value == null ? null : String.valueOf(value);
            }
            case "getInt" -> {
                var value = current(rows, index[0]).get((String) args[0]);
                wasNull[0] = value == null;
                if (value == null) {
                    yield 0;
                }
                if (value instanceof Number number) {
                    yield number.intValue();
                }
                yield Integer.parseInt(String.valueOf(value));
            }
            case "wasNull" -> wasNull[0];
            case "close" -> null;
            case "isClosed", "isWrapperFor" -> false;
            case "unwrap" -> throw new UnsupportedOperationException();
            case "toString" -> "ResultSetProxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new UnsupportedOperationException("Unsupported result-set method: " + method.getName());
        };
        return (ResultSet) Proxy.newProxyInstance(
            JdbcSchemaProviderTest.class.getClassLoader(),
            new Class[]{ResultSet.class},
            handler
        );
    }

    private static Map<String, Object> current(List<Map<String, Object>> rows, int index) {
        if (index < 0 || index >= rows.size()) {
            throw new IllegalStateException("ResultSet cursor out of range: " + index);
        }
        return rows.get(index);
    }

    private static Map<String, Object> row(Object... values) {
        var row = new LinkedHashMap<String, Object>();
        for (int i = 0; i < values.length; i += 2) {
            var key = ((String) values[i]).toUpperCase(Locale.ROOT);
            row.put(key, values[i + 1]);
        }
        return row;
    }

    @Test
    void load_readsTablesAndColumnsFromMetadata() throws SQLException {
        var state = new MetadataState();
        var tableRows = List.of(
            row("TABLE_SCHEM", "public", "TABLE_NAME", "users"),
            row("TABLE_SCHEM", "public", "TABLE_NAME", "events")
        );
        var columnRowsByTable = Map.of(
            "users", List.of(
                row("COLUMN_NAME", "id", "TYPE_NAME", "int8", "DATA_TYPE", Types.BIGINT, "ORDINAL_POSITION", 1),
                row("COLUMN_NAME", "name", "TYPE_NAME", "text", "DATA_TYPE", Types.VARCHAR, "ORDINAL_POSITION", 2)
            ),
            "events", List.of(
                row("COLUMN_NAME", "payload", "TYPE_NAME", "jsonb", "DATA_TYPE", Types.OTHER, "ORDINAL_POSITION", 1)
            )
        );
        Map<String, List<Map<String, Object>>> primaryKeyRowsByTable = Map.of(
            "users", List.of(row("COLUMN_NAME", "id", "KEY_SEQ", 1)),
            "events", List.of()
        );
        Map<String, List<Map<String, Object>>> importedKeyRowsByTable = Map.of(
            "users", List.of(),
            "events", List.of(
                row(
                    "FK_NAME", "fk_events_users",
                    "FKCOLUMN_NAME", "user_id",
                    "PKTABLE_SCHEM", "public",
                    "PKTABLE_NAME", "users",
                    "PKCOLUMN_NAME", "id",
                    "KEY_SEQ", 1
                )
            )
        );
        var metadata = metadataProxy(state, tableRows, columnRowsByTable, primaryKeyRowsByTable, importedKeyRowsByTable);
        var provider = JdbcSchemaProvider.of(dataSourceProxy(metadata), "public");

        CatalogSchema schema = provider.load();

        assertEquals("public", state.tablesSchemaPattern);
        assertNotNull(state.tablesTypes);
        assertTrue(state.tablesTypes.length >= 2);

        var usersLookup = schema.resolve("public", "users");
        var eventsLookup = schema.resolve("public", "events");
        assertTrue(usersLookup.ok());
        assertTrue(eventsLookup.ok());

        var users = ((CatalogSchema.TableLookupResult.Found) usersLookup).table();
        var events = ((CatalogSchema.TableLookupResult.Found) eventsLookup).table();
        assertEquals(CatalogType.LONG, users.column("id").orElseThrow().type());
        assertEquals(CatalogType.STRING, users.column("name").orElseThrow().type());
        assertEquals(CatalogType.JSONB, events.column("payload").orElseThrow().type());
        assertEquals(List.of("id"), users.primaryKeyColumns());
        assertEquals(1, events.foreignKeys().size());
        var fk = events.foreignKeys().getFirst();
        assertEquals("fk_events_users", fk.name());
        assertEquals("public", fk.targetSchema());
        assertEquals("users", fk.targetTable());
        assertEquals(List.of("user_id"), fk.sourceColumns());
        assertEquals(List.of("id"), fk.targetColumns());
    }

    @Test
    void load_usesDefaultMapperByDefaultOverloads() throws SQLException {
        var state = new MetadataState();
        var tableRows = List.of(row("TABLE_SCHEM", "public", "TABLE_NAME", "users"));
        var columnRowsByTable = Map.of(
            "users", List.of(
                row("COLUMN_NAME", "id", "TYPE_NAME", "integer", "DATA_TYPE", Types.INTEGER, "ORDINAL_POSITION", 1),
                row("COLUMN_NAME", "name", "TYPE_NAME", "varchar", "DATA_TYPE", Types.VARCHAR, "ORDINAL_POSITION", 2)
            )
        );
        Map<String, List<Map<String, Object>>> emptyPrimaryKeys = Map.of();
        Map<String, List<Map<String, Object>>> emptyImportedKeys = Map.of();
        var metadata = metadataProxy(state, tableRows, columnRowsByTable, emptyPrimaryKeys, emptyImportedKeys);
        var provider = JdbcSchemaProvider.of(dataSourceProxy(metadata));

        CatalogSchema schema = provider.load();
        var users = ((CatalogSchema.TableLookupResult.Found) schema.resolve("public", "users")).table();

        assertEquals(CatalogType.INTEGER, users.column("id").orElseThrow().type());
        assertEquals(CatalogType.STRING, users.column("name").orElseThrow().type());
    }

    @Test
    void of_rejectsInvalidConfiguration() {
        assertThrows(NullPointerException.class, () -> JdbcSchemaProvider.of(null));
        assertThrows(NullPointerException.class, () -> JdbcSchemaProvider.of(null, "public"));
        assertThrows(NullPointerException.class, () -> JdbcSchemaProvider.of(null, DefaultSqlTypeMapper.standard()));
        assertThrows(NullPointerException.class, () ->
            JdbcSchemaProvider.of(dataSourceProxy(metadataProxy(new MetadataState(), List.of(), Map.of(), Map.of(), Map.of())), null, null)
        );
        assertThrows(NullPointerException.class, () ->
            JdbcSchemaProvider.of(
                dataSourceProxy(metadataProxy(new MetadataState(), List.of(), Map.of(), Map.of(), Map.of())),
                null,
                null,
                List.of("TABLE"),
                null
            )
        );
        assertThrows(IllegalArgumentException.class, () ->
            JdbcSchemaProvider.of(
                dataSourceProxy(metadataProxy(new MetadataState(), List.of(), Map.of(), Map.of(), Map.of())),
                null,
                null,
                List.of(),
                DefaultSqlTypeMapper.standard()
            )
        );
        assertThrows(IllegalArgumentException.class, () ->
            JdbcSchemaProvider.of(
                dataSourceProxy(metadataProxy(new MetadataState(), List.of(), Map.of(), Map.of(), Map.of())),
                null,
                null,
                List.of(" ", "\t"),
                DefaultSqlTypeMapper.standard()
            )
        );
    }

    @Test
    void load_skipsBlankNames_normalizesTypes_andHandlesNullMetadataValues() throws SQLException {
        var state = new MetadataState();
        var tableRows = List.of(
            row("TABLE_SCHEM", " ", "TABLE_NAME", "users"),
            row("TABLE_SCHEM", "public", "TABLE_NAME", " ")
        );
        var columnRowsByTable = Map.of(
            "users", List.of(
                row("COLUMN_NAME", " ", "TYPE_NAME", "text", "DATA_TYPE", Types.VARCHAR, "ORDINAL_POSITION", 1),
                row("COLUMN_NAME", "name", "TYPE_NAME", "text", "DATA_TYPE", Types.VARCHAR, "ORDINAL_POSITION", 1),
                row("COLUMN_NAME", "payload", "TYPE_NAME", null, "DATA_TYPE", null, "ORDINAL_POSITION", null)
            )
        );
        SqlTypeMapper mapper = (nativeTypeName, jdbcType) -> {
            if (nativeTypeName == null && jdbcType == Types.OTHER) {
                return CatalogType.JSON;
            }
            return DefaultSqlTypeMapper.standard().map(nativeTypeName, jdbcType);
        };
        var provider = JdbcSchemaProvider.of(
            dataSourceProxy(metadataProxy(state, tableRows, columnRowsByTable, Map.of(), Map.of())),
            null,
            null,
            List.of("TABLE", "", "VIEW", "TABLE"),
            mapper
        );

        CatalogSchema schema = provider.load();

        assertArrayEquals(new String[]{"TABLE", "VIEW"}, state.tablesTypes);
        var users = ((CatalogSchema.TableLookupResult.Found) schema.resolve(null, "users")).table();
        assertTrue(users.column("name").isPresent());
        assertEquals(CatalogType.JSON, users.column("payload").orElseThrow().type());
        assertEquals(2, users.columns().size());
    }

    @Test
    void load_ordersCompositeKeysBySequence() throws SQLException {
        var state = new MetadataState();
        var tableRows = List.of(row("TABLE_SCHEM", "public", "TABLE_NAME", "order_items"));
        var columnRowsByTable = Map.of(
            "order_items", List.of(
                row("COLUMN_NAME", "order_id", "TYPE_NAME", "int8", "DATA_TYPE", Types.BIGINT, "ORDINAL_POSITION", 1),
                row("COLUMN_NAME", "item_id", "TYPE_NAME", "int8", "DATA_TYPE", Types.BIGINT, "ORDINAL_POSITION", 2),
                row("COLUMN_NAME", "product_id", "TYPE_NAME", "int8", "DATA_TYPE", Types.BIGINT, "ORDINAL_POSITION", 3),
                row("COLUMN_NAME", "variant_id", "TYPE_NAME", "int8", "DATA_TYPE", Types.BIGINT, "ORDINAL_POSITION", 4)
            )
        );
        var primaryKeyRowsByTable = Map.of(
            "order_items", List.of(
                row("COLUMN_NAME", "item_id", "KEY_SEQ", 2),
                row("COLUMN_NAME", "order_id", "KEY_SEQ", 1)
            )
        );
        var importedKeyRowsByTable = Map.of(
            "order_items", List.of(
                row(
                    "FK_NAME", "fk_order_items_products",
                    "FKCOLUMN_NAME", "variant_id",
                    "PKTABLE_SCHEM", "public",
                    "PKTABLE_NAME", "products",
                    "PKCOLUMN_NAME", "variant_id",
                    "KEY_SEQ", 2
                ),
                row(
                    "FK_NAME", "fk_order_items_products",
                    "FKCOLUMN_NAME", "product_id",
                    "PKTABLE_SCHEM", "public",
                    "PKTABLE_NAME", "products",
                    "PKCOLUMN_NAME", "product_id",
                    "KEY_SEQ", 1
                )
            )
        );
        var provider = JdbcSchemaProvider.of(
            dataSourceProxy(metadataProxy(state, tableRows, columnRowsByTable, primaryKeyRowsByTable, importedKeyRowsByTable)),
            null,
            null,
            List.of("TABLE", "", "VIEW", "TABLE"),
            DefaultSqlTypeMapper.standard()
        );

        CatalogSchema schema = provider.load();
        var orderItems = ((CatalogSchema.TableLookupResult.Found) schema.resolve("public", "order_items")).table();
        assertEquals(List.of("order_id", "item_id"), orderItems.primaryKeyColumns());
        assertEquals(1, orderItems.foreignKeys().size());
        var fk = orderItems.foreignKeys().getFirst();
        assertEquals(List.of("product_id", "variant_id"), fk.sourceColumns());
        assertEquals(List.of("product_id", "variant_id"), fk.targetColumns());
    }

    @Test
    void load_handles_anonymous_or_incomplete_foreign_key_rows() throws SQLException {
        var state = new MetadataState();
        var tableRows = List.of(row("TABLE_SCHEM", "public", "TABLE_NAME", "orders"));
        var columnRowsByTable = Map.of(
            "orders", List.of(
                row("COLUMN_NAME", "id", "TYPE_NAME", "int8", "DATA_TYPE", Types.BIGINT, "ORDINAL_POSITION", 1),
                row("COLUMN_NAME", "user_id", "TYPE_NAME", "int8", "DATA_TYPE", Types.BIGINT, "ORDINAL_POSITION", 2)
            )
        );
        Map<String, List<Map<String, Object>>> primaryKeys = Map.of(
            "orders", List.of(row("COLUMN_NAME", "id", "KEY_SEQ", null))
        );
        Map<String, List<Map<String, Object>>> importedKeys = Map.of(
            "orders", List.of(
                row(
                    "FK_NAME", null,
                    "FKCOLUMN_NAME", "user_id",
                    "PKTABLE_SCHEM", "public",
                    "PKTABLE_NAME", "users",
                    "PKCOLUMN_NAME", "id",
                    "KEY_SEQ", null
                ),
                row(
                    "FK_NAME", " ",
                    "FKCOLUMN_NAME", " ",
                    "PKTABLE_SCHEM", "public",
                    "PKTABLE_NAME", "users",
                    "PKCOLUMN_NAME", "id",
                    "KEY_SEQ", 1
                )
            )
        );
        var provider = JdbcSchemaProvider.of(
            dataSourceProxy(metadataProxy(state, tableRows, columnRowsByTable, primaryKeys, importedKeys)),
            "public",
            "public",
            List.of("TABLE"),
            DefaultSqlTypeMapper.standard()
        );

        var schema = provider.load();
        var orders = ((CatalogSchema.TableLookupResult.Found) schema.resolve("public", "orders")).table();
        assertEquals(List.of("id"), orders.primaryKeyColumns());
        assertEquals(1, orders.foreignKeys().size());
        assertNull(orders.foreignKeys().getFirst().name());
        assertEquals(List.of("user_id"), orders.foreignKeys().getFirst().sourceColumns());
    }

    private static final class MetadataState {
        private final List<String> requestedColumnTables = new ArrayList<>();
        private final List<String> requestedPrimaryKeyTables = new ArrayList<>();
        private final List<String> requestedImportedKeyTables = new ArrayList<>();
        private String tablesCatalog;
        private String tablesSchemaPattern;
        private String[] tablesTypes;
    }
}
