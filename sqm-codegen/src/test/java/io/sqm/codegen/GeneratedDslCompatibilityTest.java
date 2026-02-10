package io.sqm.codegen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeneratedDslCompatibilityTest {

    @TempDir
    Path tempDir;

    @Test
    void generatedSourcesCompileAgainstCurrentDsl() throws IOException {
        var sqlDir = tempDir.resolve("sql");
        var outDir = tempDir.resolve("generated");
        var classesDir = tempDir.resolve("classes");

        writeResource("golden/sql/user/a_find_by_id.sql", sqlDir.resolve("user/a_find_by_id.sql"));
        writeResource("golden/sql/user/z_list_active.sql", sqlDir.resolve("user/z_list_active.sql"));
        writeResource("golden/sql/analytics/ranked.sql", sqlDir.resolve("analytics/ranked.sql"));
        writeResource("golden/sql/reporting/kitchen_sink.sql", sqlDir.resolve("reporting/kitchen_sink.sql"));

        var options = SqlFileCodegenOptions.of(sqlDir, outDir, "io.sqm.codegen.generated");
        var generatedFiles = SqlFileCodeGenerator.of(options).generate();
        assertFalse(generatedFiles.isEmpty(), "Expected generated Java sources.");

        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "System Java compiler is required to run this test.");

        Files.createDirectories(classesDir);

        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromPaths(generatedFiles);
            var classpath = System.getProperty("java.class.path");
            var compileOptions = List.of(
                "-classpath", classpath,
                "-d", classesDir.toString()
            );
            var task = compiler.getTask(null, fileManager, diagnostics, compileOptions, null, compilationUnits);
            boolean success = Boolean.TRUE.equals(task.call());
            assertTrue(success, "Generated sources failed to compile: " + diagnostics.getDiagnostics());
        }
    }

    private void writeResource(String resourceName, Path target) throws IOException {
        var content = readResource(resourceName);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }

    private String readResource(String resourceName) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IOException("Missing test resource: " + resourceName);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

