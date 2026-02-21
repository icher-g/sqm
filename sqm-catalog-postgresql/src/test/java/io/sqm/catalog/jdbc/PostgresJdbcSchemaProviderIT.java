package io.sqm.catalog.jdbc;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogType;
import io.sqm.catalog.postgresql.PostgresSqlTypeMapper;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class PostgresJdbcSchemaProviderIT {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void load_readsSchemaFromRealPostgresMetadata() throws Exception {
        try (var connection = DriverManager.getConnection(
            POSTGRES.getJdbcUrl(),
            POSTGRES.getUsername(),
            POSTGRES.getPassword()
        )) {
            try (var statement = connection.createStatement()) {
                statement.execute("create table users (" +
                    "id bigint primary key," +
                    "name text not null," +
                    "active boolean not null," +
                    "amount numeric(12,2)," +
                    "uid uuid," +
                    "payload jsonb," +
                    "created_at timestamptz," +
                    "data bytea)");
                statement.execute("create table events (" +
                    "id bigint primary key," +
                    "user_id bigint not null references users(id)," +
                    "payload jsonb not null)");
            }
        }

        var provider = JdbcSchemaProvider.of(dataSource(), PostgresSqlTypeMapper.standard());
        CatalogSchema schema = provider.load();

        var users = ((CatalogSchema.TableLookupResult.Found) schema.resolve("public", "users")).table();
        assertEquals(CatalogType.LONG, users.column("id").orElseThrow().type());
        assertEquals(CatalogType.STRING, users.column("name").orElseThrow().type());
        assertEquals(CatalogType.BOOLEAN, users.column("active").orElseThrow().type());
        assertEquals(CatalogType.DECIMAL, users.column("amount").orElseThrow().type());
        assertEquals(CatalogType.UUID, users.column("uid").orElseThrow().type());
        assertEquals(CatalogType.JSONB, users.column("payload").orElseThrow().type());
        assertEquals(CatalogType.TIMESTAMP, users.column("created_at").orElseThrow().type());
        assertEquals(CatalogType.BYTES, users.column("data").orElseThrow().type());
        assertEquals(1, users.primaryKeyColumns().size());
        assertEquals("id", users.primaryKeyColumns().getFirst());

        var events = ((CatalogSchema.TableLookupResult.Found) schema.resolve("public", "events")).table();
        assertEquals(1, events.primaryKeyColumns().size());
        assertEquals("id", events.primaryKeyColumns().getFirst());
        assertEquals(1, events.foreignKeys().size());
        var fk = events.foreignKeys().getFirst();
        assertEquals("users", fk.targetTable());
        assertEquals(1, fk.sourceColumns().size());
        assertEquals("user_id", fk.sourceColumns().getFirst());
        assertEquals(1, fk.targetColumns().size());
        assertEquals("id", fk.targetColumns().getFirst());
    }

    private static DataSource dataSource() {
        return new DataSource() {
            @Override
            public java.sql.Connection getConnection() throws SQLException {
                return DriverManager.getConnection(
                    POSTGRES.getJdbcUrl(),
                    POSTGRES.getUsername(),
                    POSTGRES.getPassword()
                );
            }

            @Override
            public java.sql.Connection getConnection(String username, String password) throws SQLException {
                return DriverManager.getConnection(POSTGRES.getJdbcUrl(), username, password);
            }

            @Override
            public PrintWriter getLogWriter() {
                return null;
            }

            @Override
            public void setLogWriter(PrintWriter out) {
            }

            @Override
            public void setLoginTimeout(int seconds) {
            }

            @Override
            public int getLoginTimeout() {
                return 0;
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
