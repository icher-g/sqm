package io.sqm.schema.introspect.jdbc;

import io.sqm.validate.schema.model.DbSchema;
import io.sqm.validate.schema.model.DbType;
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
            }
        }

        var provider = JdbcSchemaProvider.of(dataSource());
        DbSchema schema = provider.load();

        var users = ((DbSchema.TableLookupResult.Found) schema.resolve("public", "users")).table();
        assertEquals(DbType.LONG, users.column("id").orElseThrow().type());
        assertEquals(DbType.STRING, users.column("name").orElseThrow().type());
        assertEquals(DbType.BOOLEAN, users.column("active").orElseThrow().type());
        assertEquals(DbType.DECIMAL, users.column("amount").orElseThrow().type());
        assertEquals(DbType.UUID, users.column("uid").orElseThrow().type());
        assertEquals(DbType.JSONB, users.column("payload").orElseThrow().type());
        assertEquals(DbType.TIMESTAMP, users.column("created_at").orElseThrow().type());
        assertEquals(DbType.BYTES, users.column("data").orElseThrow().type());
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
