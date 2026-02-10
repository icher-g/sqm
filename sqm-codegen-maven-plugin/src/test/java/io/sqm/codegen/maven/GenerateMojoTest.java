package io.sqm.codegen.maven;

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

