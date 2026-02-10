package io.sqm.codegen;

import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class SqlFileCodeGeneratorSemanticTest {

    private static final List<String> SQL_FIXTURES = List.of(
        "golden/sql/user/a_find_by_id.sql",
        "golden/sql/user/z_list_active.sql",
        "golden/sql/analytics/ranked.sql",
        "golden/sql/reporting/kitchen_sink.sql"
    );

    @TempDir
    Path tempDir;

    @Test
    void generate_producesQueriesEquivalentToOriginalSqlModel() throws Exception {
        var sqlDir = tempDir.resolve("sql");
        var outDir = tempDir.resolve("generated");
        var expectedAndSource = writeFixturesAndParseExpected(sqlDir);

        var options = SqlFileCodegenOptions.of(sqlDir, outDir, "io.sqm.codegen.generated");
        var generated = SqlFileCodeGenerator.of(options).generate();
        var classOutputDir = compileGeneratedSources(generated, tempDir.resolve("compiled"));

        try (var classLoader = URLClassLoader.newInstance(new URL[]{classOutputDir.toUri().toURL()}, getClass().getClassLoader())) {
            for (var entry : expectedAndSource) {
                var source = entry.source();
                var expected = entry.query();
                var className = NameNormalizer.toClassName(source.folder());
                var fqcn = options.basePackage() + "." + className;
                var methodName = source.methodName();
                var actual = invokeQueryMethod(classLoader, fqcn, methodName);

                assertEquals(expected, actual, "Generated query mismatch for " + source.relativePath());

                if (expected instanceof SelectQuery expectedSelect && actual instanceof SelectQuery actualSelect) {
                    assertEquals(
                        expectedSelect.lockFor(),
                        actualSelect.lockFor(),
                        "Generated locking clause mismatch for " + source.relativePath()
                    );
                }
            }
        }
    }

    private List<ExpectedQuery> writeFixturesAndParseExpected(Path sqlDir) throws IOException {
        var out = new ArrayList<ExpectedQuery>();
        for (var resource : SQL_FIXTURES) {
            var relative = resource.substring("golden/sql/".length());
            var target = sqlDir.resolve(relative);
            var sql = readResource(resource);
            Files.createDirectories(target.getParent());
            Files.writeString(target, sql, StandardCharsets.UTF_8);

            var relativePath = Path.of(relative);
            var fileName = target.getFileName().toString();
            var baseName = fileName.substring(0, fileName.length() - 4);
            var methodName = NameNormalizer.toMethodName(baseName);
            var folder = relativePath.getParent();
            var source = new FixtureSource(relativePath, folder == null ? Path.of("") : folder, methodName);
            out.add(new ExpectedQuery(source, parseSql(sql, relativePath)));
        }
        return out;
    }

    private Query parseSql(String sql, Path relativePath) {
        var parseContexts = List.of(
            ParseContext.of(new AnsiSpecs()),
            ParseContext.of(new PostgresSpecs())
        );
        ParseResult<? extends Query> firstError = null;
        for (var parseContext : parseContexts) {
            var result = parseContext.parse(Query.class, sql);
            if (result.ok() && result.value() != null) {
                return result.value();
            }
            if (firstError == null) {
                firstError = result;
            }
        }
        if (!firstError.problems().isEmpty()) {
            fail("Unable to parse fixture " + relativePath + ": " + firstError.problems().getFirst().message());
        }
        fail("Unable to parse fixture " + relativePath + ": unknown parse error");
        return null;
    }

    private Path compileGeneratedSources(List<Path> generatedSources, Path classOutputDir) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "JDK compiler is required for semantic codegen tests");

        Files.createDirectories(classOutputDir);
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            var units = fileManager.getJavaFileObjectsFromFiles(generatedSources.stream().map(Path::toFile).toList());
            var options = List.of(
                "-classpath", System.getProperty("java.class.path"),
                "-d", classOutputDir.toString()
            );
            var success = compiler.getTask(null, fileManager, diagnostics, options, null, units).call();
            if (!Boolean.TRUE.equals(success)) {
                var message = new StringBuilder("Compilation of generated sources failed:\n");
                diagnostics.getDiagnostics().forEach(diagnostic -> message.append(diagnostic).append('\n'));
                fail(message.toString());
            }
        }
        return classOutputDir;
    }

    private Query invokeQueryMethod(ClassLoader classLoader, String fqcn, String methodName) throws ClassNotFoundException {
        try {
            var generatedClass = Class.forName(fqcn, true, classLoader);
            var method = generatedClass.getMethod(methodName);
            var value = method.invoke(null);
            return assertInstanceOf(Query.class, value);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to invoke generated method " + fqcn + "." + methodName + "()", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Generated method threw " + e.getTargetException().getClass().getSimpleName()
                + " for " + fqcn + "." + methodName + "()", e);
        }
    }

    private String readResource(String resourceName) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IOException("Missing test resource: " + resourceName);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private record FixtureSource(Path relativePath, Path folder, String methodName) {
    }

    private record ExpectedQuery(FixtureSource source, Query query) {
    }
}
