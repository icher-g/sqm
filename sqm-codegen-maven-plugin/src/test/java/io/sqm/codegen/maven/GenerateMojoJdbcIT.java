package io.sqm.codegen.maven;

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test that validates SQL code generation with JDBC schema provider
 * against a real PostgreSQL container.
 */
@Testcontainers
class GenerateMojoJdbcIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("sqm")
        .withUsername("sqm")
        .withPassword("sqm");

    @TempDir
    Path tempDir;

    @Test
    void jdbcSchemaProviderValidatesAndGeneratesSourcesAgainstRealPostgres() throws Exception {
        try (var connection = DriverManager.getConnection(
            POSTGRES.getJdbcUrl(),
            POSTGRES.getUsername(),
            POSTGRES.getPassword()
        );
             var statement = connection.createStatement()) {
            statement.execute("create table if not exists users (id bigint primary key, user_name text not null)");
        }

        var sqlDir = tempDir.resolve("sql");
        var outDir = tempDir.resolve("generated");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(
            sqlDir.resolve("user/find_by_id.sql"),
            "select u.id, u.user_name from users u where u.id = :id",
            StandardCharsets.UTF_8
        );

        var mojo = new GenerateMojo();
        setField(mojo, "project", new MavenProject());
        setField(mojo, "skip", false);
        setField(mojo, "dialect", "postgresql");
        setField(mojo, "basePackage", "io.sqm.codegen.generated");
        setField(mojo, "sqlDirectory", sqlDir.toString());
        setField(mojo, "generatedSourcesDirectory", outDir.toString());
        setField(mojo, "cleanupStaleFiles", true);
        setField(mojo, "includeGenerationTimestamp", false);
        setField(mojo, "schemaProvider", "jdbc");
        setField(mojo, "schemaJdbcUrl", POSTGRES.getJdbcUrl());
        setField(mojo, "schemaJdbcUsername", POSTGRES.getUsername());
        setField(mojo, "schemaJdbcPassword", POSTGRES.getPassword());
        setField(mojo, "schemaJdbcSchemaPattern", "public");
        setField(mojo, "schemaCacheRefresh", true);
        setField(mojo, "schemaCacheWrite", false);
        setField(mojo, "validationReportPath", tempDir.resolve("reports/validation.json").toString());

        mojo.execute();

        assertTrue(Files.exists(outDir.resolve("io/sqm/codegen/generated/UserQueries.java")));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

