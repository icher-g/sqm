package io.sqm.middleware.core;

import io.sqm.catalog.jdbc.JdbcSchemaProvider;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.snapshot.JsonSchemaProvider;
import io.sqm.control.ConfigKeys;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.logging.Logger;

final class SchemaBootstrapLoader {

    private static final String DEFAULT_SCHEMA_RESOURCE = "/io/sqm/middleware/core/default-schema.json";

    private final BiFunction<ConfigKeys.Key, String, String> readString;

    SchemaBootstrapLoader(BiFunction<ConfigKeys.Key, String, String> readString) {
        this.readString = Objects.requireNonNull(readString, "readString must not be null");
    }

    BootstrapResult bootstrap(String source, boolean failFast) {
        try {
            var schemaLoad = load(source);
            return BootstrapResult.ready(source, schemaLoad);
        } catch (RuntimeException ex) {
            var message = "Schema bootstrap failed [source=%s]: %s".formatted(source, ex.getMessage());
            if (failFast) {
                if (ex instanceof IllegalArgumentException) {
                    throw new IllegalArgumentException(message, ex);
                }
                if (ex instanceof IllegalStateException) {
                    throw new IllegalStateException(message, ex);
                }
                throw new IllegalStateException(message, ex);
            }
            return BootstrapResult.degraded(source, message);
        }
    }

    private SchemaLoadResult load(String source) {
        return switch (source) {
            case "json" -> loadJsonSchema();
            case "jdbc" -> loadJdbcSchema();
            case "manual" -> loadDefaultJsonSchema();
            default -> throw new IllegalArgumentException("Unsupported schema source '%s'. Supported: manual,json,jdbc".formatted(source));
        };
    }

    private SchemaLoadResult loadDefaultJsonSchema() {
        var defaultPath = readString.apply(ConfigKeys.SCHEMA_DEFAULT_JSON_PATH, null);
        if (defaultPath != null && !defaultPath.isBlank()) {
            return loadJsonSchema(Path.of(defaultPath));
        }

        try (InputStream stream = SchemaBootstrapLoader.class.getResourceAsStream(DEFAULT_SCHEMA_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled default schema resource: " + DEFAULT_SCHEMA_RESOURCE);
            }

            var tempFile = Files.createTempFile("sqm-default-schema", ".json");
            Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            tempFile.toFile().deleteOnExit();
            var schema = loadJsonSchema(tempFile);
            return new SchemaLoadResult(schema.schema(), "manual bundled resource %s".formatted(DEFAULT_SCHEMA_RESOURCE));
        } catch (IOException ex) {
            throw new IllegalStateException(
                "Failed to load bundled default schema JSON resource '%s'. Configure %s or %s to an explicit path."
                    .formatted(
                        DEFAULT_SCHEMA_RESOURCE,
                        ConfigKeys.SCHEMA_DEFAULT_JSON_PATH.property(),
                        ConfigKeys.SCHEMA_DEFAULT_JSON_PATH.env()
                    ),
                ex
            );
        }
    }

    private SchemaLoadResult loadJsonSchema() {
        var path = readString.apply(ConfigKeys.SCHEMA_JSON_PATH, null);
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException(
                "JSON schema source requires %s (or %s)".formatted(
                    ConfigKeys.SCHEMA_JSON_PATH.property(),
                    ConfigKeys.SCHEMA_JSON_PATH.env()
                )
            );
        }
        return loadJsonSchema(Path.of(path));
    }

    private SchemaLoadResult loadJsonSchema(Path path) {
        try {
            var schema = JsonSchemaProvider.of(path).load();
            return new SchemaLoadResult(schema, "json file %s".formatted(path));
        } catch (SQLException ex) {
            throw new IllegalStateException(
                "Failed to load schema from JSON path '%s'. Verify file exists and is valid schema JSON."
                    .formatted(path),
                ex
            );
        }
    }

    private SchemaLoadResult loadJdbcSchema() {
        var url = required(ConfigKeys.JDBC_URL);
        var user = readString.apply(ConfigKeys.JDBC_USER, "");
        var password = readString.apply(ConfigKeys.JDBC_PASSWORD, "");
        var schemaPattern = readString.apply(ConfigKeys.JDBC_SCHEMA_PATTERN, null);
        var driverClass = readString.apply(ConfigKeys.JDBC_DRIVER, null);

        if (driverClass != null && !driverClass.isBlank()) {
            try {
                Class.forName(driverClass);
            } catch (ClassNotFoundException ex) {
                throw new IllegalArgumentException(
                    "JDBC driver class not found '%s'. Configure %s/%s with a valid driver."
                        .formatted(driverClass, ConfigKeys.JDBC_DRIVER.property(), ConfigKeys.JDBC_DRIVER.env()),
                    ex
                );
            }
        }

        DataSource dataSource = new DriverManagerDataSource(url, user, password);
        try {
            var schema = JdbcSchemaProvider.of(dataSource, schemaPattern).load();
            return new SchemaLoadResult(
                schema,
                schemaPattern == null || schemaPattern.isBlank()
                    ? "jdbc metadata " + url
                    : "jdbc metadata %s (schemaPattern=%s)".formatted(url, schemaPattern)
            );
        } catch (SQLException ex) {
            throw new IllegalStateException(
                "Failed to load schema from JDBC metadata for '%s'. Verify connectivity and privileges.".formatted(url),
                ex
            );
        }
    }

    private String required(ConfigKeys.Key key) {
        var value = readString.apply(key, null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required configuration: " + key.property() + " / " + key.env());
        }
        return value;
    }

    record DriverManagerDataSource(String url, String user, String password) implements DataSource {
        DriverManagerDataSource(String url, String user, String password) {
            this.url = Objects.requireNonNull(url, "url must not be null");
            this.user = user;
            this.password = password;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url, user, password);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return DriverManager.getConnection(url, username, password);
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLFeatureNotSupportedException("unwrap is not supported");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }

        @Override
        public PrintWriter getLogWriter() {
            return DriverManager.getLogWriter();
        }

        @Override
        public void setLogWriter(PrintWriter out) {
            DriverManager.setLogWriter(out);
        }

        @Override
        public int getLoginTimeout() {
            return DriverManager.getLoginTimeout();
        }

        @Override
        public void setLoginTimeout(int seconds) {
            DriverManager.setLoginTimeout(seconds);
        }

        @Override
        public Logger getParentLogger() {
            return Logger.getLogger("global");
        }
    }

    record SchemaLoadResult(CatalogSchema schema, String description) {
        SchemaLoadResult {
            Objects.requireNonNull(schema, "schema must not be null");
            Objects.requireNonNull(description, "description must not be null");
            if (description.isBlank()) {
                throw new IllegalArgumentException("description must not be blank");
            }
        }
    }

    record BootstrapResult(String source, SchemaLoadResult schemaLoad, String degradedMessage) {
        BootstrapResult {
            Objects.requireNonNull(source, "source must not be null");
            if (source.isBlank()) {
                throw new IllegalArgumentException("source must not be blank");
            }
            if (schemaLoad == null && (degradedMessage == null || degradedMessage.isBlank())) {
                throw new IllegalArgumentException("degradedMessage must be set when schemaLoad is null");
            }
            if (schemaLoad != null && degradedMessage != null) {
                throw new IllegalArgumentException("degradedMessage must be null when schemaLoad is set");
            }
        }

        static BootstrapResult ready(String source, SchemaLoadResult schemaLoad) {
            return new BootstrapResult(source, Objects.requireNonNull(schemaLoad, "schemaLoad must not be null"), null);
        }

        static BootstrapResult degraded(String source, String degradedMessage) {
            return new BootstrapResult(source, null, Objects.requireNonNull(degradedMessage, "degradedMessage must not be null"));
        }

        boolean ready() {
            return schemaLoad != null;
        }
    }
}
