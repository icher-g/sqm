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

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
