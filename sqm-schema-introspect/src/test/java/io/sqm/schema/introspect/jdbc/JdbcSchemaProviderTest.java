package io.sqm.schema.introspect.jdbc;

import io.sqm.schema.introspect.postgresql.PostgresSqlTypeMapper;
import io.sqm.validate.schema.model.DbSchema;
import io.sqm.validate.schema.model.DbType;
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
        Map<String, List<Map<String, Object>>> columnRowsByTable
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
        var metadata = metadataProxy(state, tableRows, columnRowsByTable);
        var provider = JdbcSchemaProvider.of(dataSourceProxy(metadata), "public");

        DbSchema schema = provider.load();

        assertEquals("public", state.tablesSchemaPattern);
        assertNotNull(state.tablesTypes);
        assertTrue(state.tablesTypes.length >= 2);

        var usersLookup = schema.resolve("public", "users");
        var eventsLookup = schema.resolve("public", "events");
        assertTrue(usersLookup.ok());
        assertTrue(eventsLookup.ok());

        var users = ((DbSchema.TableLookupResult.Found) usersLookup).table();
        var events = ((DbSchema.TableLookupResult.Found) eventsLookup).table();
        assertEquals(DbType.LONG, users.column("id").orElseThrow().type());
        assertEquals(DbType.STRING, users.column("name").orElseThrow().type());
        assertEquals(DbType.JSONB, events.column("payload").orElseThrow().type());
    }

    @Test
    void of_rejectsInvalidConfiguration() {
        assertThrows(NullPointerException.class, () -> JdbcSchemaProvider.of(null));
        assertThrows(NullPointerException.class, () ->
            JdbcSchemaProvider.of(
                dataSourceProxy(metadataProxy(new MetadataState(), List.of(), Map.of())),
                null,
                null,
                List.of("TABLE"),
                null
            )
        );
        assertThrows(IllegalArgumentException.class, () ->
            JdbcSchemaProvider.of(
                dataSourceProxy(metadataProxy(new MetadataState(), List.of(), Map.of())),
                null,
                null,
                List.of(),
                PostgresSqlTypeMapper.standard()
            )
        );
    }

    private static final class MetadataState {
        private final List<String> requestedColumnTables = new ArrayList<>();
        private String tablesCatalog;
        private String tablesSchemaPattern;
        private String[] tablesTypes;
    }
}

