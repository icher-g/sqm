package io.sqm.codegen.maven;

import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GenerateMojoTest {

    @TempDir
    Path tempDir;

    @Test
    void removesStaleGeneratedFilesWhenEnabled() throws Exception {
        var sqlDir = tempDir.resolve("sql");
        var outDir = tempDir.resolve("generated");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(
            sqlDir.resolve("user/find_by_id.sql"),
            "select * from users where id = :id",
            StandardCharsets.UTF_8
        );

        var staleFile = outDir.resolve("io/sqm/codegen/generated/StaleQueries.java");
        Files.createDirectories(staleFile.getParent());
        Files.writeString(staleFile, "class StaleQueries {}", StandardCharsets.UTF_8);

        var mojo = new GenerateMojo();
        setField(mojo, "project", new MavenProject());
        setField(mojo, "skip", false);
        setField(mojo, "dialect", "ansi");
        setField(mojo, "basePackage", "io.sqm.codegen.generated");
        setField(mojo, "sqlDirectory", sqlDir.toString());
        setField(mojo, "generatedSourcesDirectory", outDir.toString());
        setField(mojo, "cleanupStaleFiles", true);

        mojo.execute();

        assertFalse(Files.exists(staleFile));
        assertTrue(Files.exists(outDir.resolve("io/sqm/codegen/generated/UserQueries.java")));
    }

    @Test
    void keepsStaleGeneratedFilesWhenDisabled() throws Exception {
        var sqlDir = tempDir.resolve("sql2");
        var outDir = tempDir.resolve("generated2");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(
            sqlDir.resolve("user/find_by_id.sql"),
            "select * from users where id = :id",
            StandardCharsets.UTF_8
        );

        var staleFile = outDir.resolve("io/sqm/codegen/generated/StaleQueries.java");
        Files.createDirectories(staleFile.getParent());
        Files.writeString(staleFile, "class StaleQueries {}", StandardCharsets.UTF_8);

        var mojo = new GenerateMojo();
        setField(mojo, "project", new MavenProject());
        setField(mojo, "skip", false);
        setField(mojo, "dialect", "ansi");
        setField(mojo, "basePackage", "io.sqm.codegen.generated");
        setField(mojo, "sqlDirectory", sqlDir.toString());
        setField(mojo, "generatedSourcesDirectory", outDir.toString());
        setField(mojo, "cleanupStaleFiles", false);

        mojo.execute();

        assertTrue(Files.exists(staleFile));
        assertTrue(Files.exists(outDir.resolve("io/sqm/codegen/generated/UserQueries.java")));
    }

    @Test
    void skipFlagSkipsGeneration() throws Exception {
        var outDir = tempDir.resolve("generated-skip");
        var mojo = new GenerateMojo();
        var project = new MavenProject();
        int sourceRootsBefore = project.getCompileSourceRoots().size();

        setField(mojo, "project", project);
        setField(mojo, "skip", true);
        setField(mojo, "dialect", "ansi");
        setField(mojo, "basePackage", "io.sqm.codegen.generated");
        setField(mojo, "sqlDirectory", tempDir.resolve("sql-skip").toString());
        setField(mojo, "generatedSourcesDirectory", outDir.toString());
        setField(mojo, "cleanupStaleFiles", true);
        setField(mojo, "includeGenerationTimestamp", false);

        mojo.execute();

        assertFalse(Files.exists(outDir));
        assertEquals(project.getCompileSourceRoots().size(), sourceRootsBefore);
    }

    @Test
    void invalidDialectIsReportedAsExecutionFailure() throws Exception {
        var mojo = new GenerateMojo();
        setField(mojo, "project", new MavenProject());
        setField(mojo, "skip", false);
        setField(mojo, "dialect", "bad_dialect");
        setField(mojo, "basePackage", "io.sqm.codegen.generated");
        setField(mojo, "sqlDirectory", tempDir.resolve("sql-invalid-dialect").toString());
        setField(mojo, "generatedSourcesDirectory", tempDir.resolve("generated-invalid-dialect").toString());
        setField(mojo, "cleanupStaleFiles", true);
        setField(mojo, "includeGenerationTimestamp", false);

        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test
    void invalidSqlIsReportedAsMojoFailure() throws Exception {
        var sqlDir = tempDir.resolve("sql-invalid");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(sqlDir.resolve("user").resolve("broken.sql"), "select from", StandardCharsets.UTF_8);

        var mojo = new GenerateMojo();
        setField(mojo, "project", new MavenProject());
        setField(mojo, "skip", false);
        setField(mojo, "dialect", "ansi");
        setField(mojo, "basePackage", "io.sqm.codegen.generated");
        setField(mojo, "sqlDirectory", sqlDir.toString());
        setField(mojo, "generatedSourcesDirectory", tempDir.resolve("generated-invalid").toString());
        setField(mojo, "cleanupStaleFiles", true);
        setField(mojo, "includeGenerationTimestamp", false);

        var error = assertThrows(MojoFailureException.class, mojo::execute);
        assertTrue(error.getMessage().contains("SQL code generation failed"));
    }

    @Test
    void includeGenerationTimestampAddsGeneratedDateMetadata() throws Exception {
        var sqlDir = tempDir.resolve("sql-date");
        var outDir = tempDir.resolve("generated-date");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(sqlDir.resolve("user/find_by_id.sql"), "select * from users where id = :id", StandardCharsets.UTF_8);

        var mojo = new GenerateMojo();
        setField(mojo, "project", new MavenProject());
        setField(mojo, "skip", false);
        setField(mojo, "dialect", "ansi");
        setField(mojo, "basePackage", "io.sqm.codegen.generated");
        setField(mojo, "sqlDirectory", sqlDir.toString());
        setField(mojo, "generatedSourcesDirectory", outDir.toString());
        setField(mojo, "cleanupStaleFiles", true);
        setField(mojo, "includeGenerationTimestamp", true);

        mojo.execute();

        var generatedFile = outDir.resolve("io/sqm/codegen/generated/UserQueries.java");
        var source = Files.readString(generatedFile, StandardCharsets.UTF_8);
        assertTrue(source.contains("@Generated("));
        assertTrue(source.contains("date = \""));
    }

    @Test
    void jsonSchemaProviderEnablesSemanticValidation() throws Exception {
        var sqlDir = tempDir.resolve("sql-schema-ok");
        var outDir = tempDir.resolve("generated-schema-ok");
        var schemaSnapshot = tempDir.resolve("schema-ok.json");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(sqlDir.resolve("user/find_by_id.sql"), "select u.id from users u where u.id = :id", StandardCharsets.UTF_8);
        Files.writeString(schemaSnapshot, """
            {
              "tables": [
                {
                  "schema": "public",
                  "name": "users",
                  "columns": [
                    { "name": "id", "type": "LONG" }
                  ]
                }
              ]
            }
            """, StandardCharsets.UTF_8);

        var mojo = new GenerateMojo();
        setField(mojo, "project", new MavenProject());
        setField(mojo, "skip", false);
        setField(mojo, "dialect", "postgresql");
        setField(mojo, "basePackage", "io.sqm.codegen.generated");
        setField(mojo, "sqlDirectory", sqlDir.toString());
        setField(mojo, "generatedSourcesDirectory", outDir.toString());
        setField(mojo, "cleanupStaleFiles", true);
        setField(mojo, "includeGenerationTimestamp", false);
        setField(mojo, "schemaProvider", "json");
        setField(mojo, "schemaSnapshotPath", schemaSnapshot.toString());

        mojo.execute();

        assertTrue(Files.exists(outDir.resolve("io/sqm/codegen/generated/UserQueries.java")));
    }

    @Test
    void jsonSchemaProviderFailsOnMissingColumn() throws Exception {
        var sqlDir = tempDir.resolve("sql-schema-fail");
        var outDir = tempDir.resolve("generated-schema-fail");
        var schemaSnapshot = tempDir.resolve("schema-fail.json");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(sqlDir.resolve("user/find_by_id.sql"), "select u.missing_col from users u", StandardCharsets.UTF_8);
        Files.writeString(schemaSnapshot, """
            {
              "tables": [
                {
                  "schema": "public",
                  "name": "users",
                  "columns": [
                    { "name": "id", "type": "LONG" }
                  ]
                }
              ]
            }
            """, StandardCharsets.UTF_8);

        var mojo = new GenerateMojo();
        setField(mojo, "project", new MavenProject());
        setField(mojo, "skip", false);
        setField(mojo, "dialect", "postgresql");
        setField(mojo, "basePackage", "io.sqm.codegen.generated");
        setField(mojo, "sqlDirectory", sqlDir.toString());
        setField(mojo, "generatedSourcesDirectory", outDir.toString());
        setField(mojo, "cleanupStaleFiles", true);
        setField(mojo, "includeGenerationTimestamp", false);
        setField(mojo, "schemaProvider", "json");
        setField(mojo, "schemaSnapshotPath", schemaSnapshot.toString());

        var error = assertThrows(MojoFailureException.class, mojo::execute);

        assertTrue(error.getMessage().contains("semantic validation failed"));
        assertTrue(error.getMessage().contains("COLUMN_NOT_FOUND"));
    }

    @Test
    void jdbcSchemaProvider_usesCacheWhenPresent() throws Exception {
        var sqlDir = tempDir.resolve("sql-jdbc-cache");
        var outDir = tempDir.resolve("generated-jdbc-cache");
        var cacheFile = tempDir.resolve("schema-cache.json");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(sqlDir.resolve("user/find_by_id.sql"), "select u.id from users u where u.id = :id", StandardCharsets.UTF_8);
        Files.writeString(cacheFile, """
            {
              "tables": [
                {
                  "schema": "public",
                  "name": "users",
                  "columns": [
                    { "name": "id", "type": "LONG" }
                  ]
                }
              ]
            }
            """, StandardCharsets.UTF_8);

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
        setField(mojo, "schemaJdbcUrl", "jdbc:postgresql://invalid-host:5432/invalid");
        setField(mojo, "schemaCachePath", cacheFile.toString());
        setField(mojo, "schemaCacheRefresh", false);
        setField(mojo, "schemaCacheWrite", true);

        mojo.execute();

        assertTrue(Files.exists(outDir.resolve("io/sqm/codegen/generated/UserQueries.java")));
    }

    @Test
    void jdbcSchemaProvider_refreshBypassesCacheAndFailsForInvalidJdbc() throws Exception {
        var sqlDir = tempDir.resolve("sql-jdbc-refresh");
        var outDir = tempDir.resolve("generated-jdbc-refresh");
        var cacheFile = tempDir.resolve("schema-refresh-cache.json");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(sqlDir.resolve("user/find_by_id.sql"), "select u.id from users u where u.id = :id", StandardCharsets.UTF_8);
        Files.writeString(cacheFile, """
            {
              "tables": [
                {
                  "schema": "public",
                  "name": "users",
                  "columns": [
                    { "name": "id", "type": "LONG" }
                  ]
                }
              ]
            }
            """, StandardCharsets.UTF_8);

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
        setField(mojo, "schemaJdbcUrl", "jdbc:postgresql://invalid-host:5432/invalid");
        setField(mojo, "schemaCachePath", cacheFile.toString());
        setField(mojo, "schemaCacheRefresh", true);
        setField(mojo, "schemaCacheWrite", true);

        var error = assertThrows(MojoFailureException.class, mojo::execute);

        assertTrue(error.getMessage().contains("Failed to load schema for validation"));
    }

    @Test
    void semanticValidationCanBeConfiguredAsNonFailingAndWritesReport() throws Exception {
        var sqlDir = tempDir.resolve("sql-schema-non-fail");
        var outDir = tempDir.resolve("generated-schema-non-fail");
        var reportPath = tempDir.resolve("reports/validation.json");
        var schemaSnapshot = tempDir.resolve("schema-non-fail.json");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(sqlDir.resolve("user/find_by_id.sql"), "select u.missing_col from users u", StandardCharsets.UTF_8);
        Files.writeString(schemaSnapshot, """
            {
              "tables": [
                {
                  "schema": "public",
                  "name": "users",
                  "columns": [
                    { "name": "id", "type": "LONG" }
                  ]
                }
              ]
            }
            """, StandardCharsets.UTF_8);

        var mojo = new GenerateMojo();
        setField(mojo, "project", new MavenProject());
        setField(mojo, "skip", false);
        setField(mojo, "dialect", "postgresql");
        setField(mojo, "basePackage", "io.sqm.codegen.generated");
        setField(mojo, "sqlDirectory", sqlDir.toString());
        setField(mojo, "generatedSourcesDirectory", outDir.toString());
        setField(mojo, "cleanupStaleFiles", true);
        setField(mojo, "includeGenerationTimestamp", false);
        setField(mojo, "schemaProvider", "json");
        setField(mojo, "schemaSnapshotPath", schemaSnapshot.toString());
        setField(mojo, "failOnValidationError", false);
        setField(mojo, "validationReportPath", reportPath.toString());

        mojo.execute();

        var report = Files.readString(reportPath, StandardCharsets.UTF_8);
        assertTrue(report.contains("\"issuesCount\": 1"));
        assertTrue(report.contains("\"COLUMN_NOT_FOUND\""));
        assertTrue(Files.exists(outDir.resolve("io/sqm/codegen/generated/UserQueries.java")));
    }

    @Test
    void jdbcSchemaProvider_skipsExpiredCacheAndFailsForInvalidJdbc() throws Exception {
        var sqlDir = tempDir.resolve("sql-jdbc-expired");
        var outDir = tempDir.resolve("generated-jdbc-expired");
        var cacheFile = tempDir.resolve("schema-expired-cache.json");
        var cacheMeta = tempDir.resolve("schema-expired-cache.json.meta.properties");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(sqlDir.resolve("user/find_by_id.sql"), "select u.id from users u where u.id = :id", StandardCharsets.UTF_8);
        Files.writeString(cacheFile, """
            {
              "tables": [
                {
                  "schema": "public",
                  "name": "users",
                  "columns": [
                    { "name": "id", "type": "LONG" }
                  ]
                }
              ]
            }
            """, StandardCharsets.UTF_8);
        Files.writeString(cacheMeta, """
            formatVersion=1
            dialect=POSTGRESQL
            databaseProduct=PostgreSQL
            databaseMajorVersion=16
            generatedAtEpochMillis=1
            """, StandardCharsets.UTF_8);
        Files.setLastModifiedTime(cacheFile, java.nio.file.attribute.FileTime.from(Instant.ofEpochSecond(1)));

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
        setField(mojo, "schemaJdbcUrl", "jdbc:postgresql://invalid-host:5432/invalid");
        setField(mojo, "schemaCachePath", cacheFile.toString());
        setField(mojo, "schemaCacheRefresh", false);
        setField(mojo, "schemaCacheWrite", true);
        setField(mojo, "schemaCacheTtlMinutes", 1L);

        var error = assertThrows(MojoFailureException.class, mojo::execute);

        assertTrue(error.getMessage().contains("Failed to load schema for validation"));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
