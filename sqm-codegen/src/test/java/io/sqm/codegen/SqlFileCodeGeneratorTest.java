package io.sqm.codegen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SqlFileCodeGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void generate_producesDeterministicClassAndMethods() throws IOException {
        var sqlDir = tempDir.resolve("sql");
        var outputDir = tempDir.resolve("generated");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(sqlDir.resolve("user").resolve("z_list_active.sql"), "select * from users where status = :status");
        Files.writeString(sqlDir.resolve("user").resolve("a_find_by_id.sql"), "select * from users where id = :id and status = :status");

        var options = SqlFileCodegenOptions.of(sqlDir, outputDir, "io.sqm.codegen.generated");
        var generated = SqlFileCodeGenerator.of(options).generate();

        assertEquals(1, generated.size());
        var generatedFile = outputDir.resolve(Path.of("io", "sqm", "codegen", "generated", "UserQueries.java"));
        assertEquals(List.of(generatedFile), generated);
        var source = Files.readString(generatedFile);
        assertTrue(source.contains("import static io.sqm.dsl.Dsl.*;"));
        assertTrue(source.contains("public static Query aFindById()"));
        assertTrue(source.contains("return select("));
        assertTrue(source.contains("param(\"id\")"));
        assertTrue(source.contains("param(\"status\")"));
        assertTrue(source.contains(".where("));
        assertTrue(source.contains("public static Set<String> aFindByIdParams()"));
        assertTrue(source.contains("return Set.of(\"id\", \"status\")"));
        assertTrue(source.contains("public static Query zListActive()"));
        assertTrue(source.contains("param(\"status\")"));
        assertTrue(source.contains("public static Set<String> zListActiveParams()"));
        assertTrue(source.contains("return Set.of(\"status\")"));
        assertTrue(source.indexOf("public static Query aFindById()") < source.indexOf("public static Query zListActive()"));
        assertFalse(source.contains("UnsupportedOperationException"));
    }

    @Test
    void generate_throwsOnClassNameCollision() throws IOException {
        var sqlDir = tempDir.resolve("sql");
        var outputDir = tempDir.resolve("generated");
        Files.createDirectories(sqlDir.resolve("foo-bar"));
        Files.createDirectories(sqlDir.resolve("foo_bar"));
        Files.writeString(sqlDir.resolve("foo-bar").resolve("a.sql"), "select 1");
        Files.writeString(sqlDir.resolve("foo_bar").resolve("b.sql"), "select 2");

        var options = SqlFileCodegenOptions.of(sqlDir, outputDir, "io.sqm.codegen.generated");

        var error = assertThrows(SqlFileCodegenException.class, () -> SqlFileCodeGenerator.of(options).generate());
        assertTrue(error.getMessage().contains("Class name collision"));
        assertTrue(error.getMessage().contains("'foo-bar' and 'foo_bar'"));
    }

    @Test
    void generate_throwsOnMethodNameCollision() throws IOException {
        var sqlDir = tempDir.resolve("sql");
        var outputDir = tempDir.resolve("generated");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(sqlDir.resolve("user").resolve("find-by-id.sql"), "select 1");
        Files.writeString(sqlDir.resolve("user").resolve("find_by_id.sql"), "select 2");

        var options = SqlFileCodegenOptions.of(sqlDir, outputDir, "io.sqm.codegen.generated");

        var error = assertThrows(SqlFileCodegenException.class, () -> SqlFileCodeGenerator.of(options).generate());
        assertTrue(error.getMessage().contains("Method name collision"));
        assertTrue(error.getMessage().contains("'user/find-by-id.sql' and 'user/find_by_id.sql'"));
    }

    @Test
    void generate_throwsWithFileLineAndColumnOnInvalidSql() throws IOException {
        var sqlDir = tempDir.resolve("sql");
        var outputDir = tempDir.resolve("generated");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(sqlDir.resolve("user").resolve("broken.sql"), "select from");

        var options = SqlFileCodegenOptions.of(sqlDir, outputDir, "io.sqm.codegen.generated");

        var error = assertThrows(SqlFileCodegenException.class, () -> SqlFileCodeGenerator.of(options).generate());
        assertTrue(error.getMessage().contains("user/broken.sql:1:"));
        assertTrue(error.getMessage().contains("stage="));
        assertTrue(error.getMessage().contains("token="));
    }

    @Test
    void generate_parsesAcrossDialectsWithoutStrictRejection() throws IOException {
        var sqlDir = tempDir.resolve("sql");
        var outputDir = tempDir.resolve("generated");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(
            sqlDir.resolve("user").resolve("distinct_on.sql"),
            "select distinct on (u.id) u.id from users u where u.id = :id"
        );

        var ansiOptions = SqlFileCodegenOptions.of(sqlDir, outputDir, "io.sqm.codegen.generated", SqlCodegenDialect.ANSI);
        var ansiGenerated = SqlFileCodeGenerator.of(ansiOptions).generate();
        assertEquals(1, ansiGenerated.size());

        var pgOptions = SqlFileCodegenOptions.of(sqlDir, outputDir, "io.sqm.codegen.generated", SqlCodegenDialect.POSTGRESQL);
        var generated = SqlFileCodeGenerator.of(pgOptions).generate();

        assertEquals(1, generated.size());
    }

    @Test
    void generate_emitsOverClause() throws IOException {
        var sqlDir = tempDir.resolve("sql");
        var outputDir = tempDir.resolve("generated");
        Files.createDirectories(sqlDir.resolve("analytics"));
        Files.writeString(
            sqlDir.resolve("analytics").resolve("ranked.sql"),
            "select row_number() over (partition by dept order by salary desc) as rn from employees"
        );

        var options = SqlFileCodegenOptions.of(sqlDir, outputDir, "io.sqm.codegen.generated");
        var generated = SqlFileCodeGenerator.of(options).generate();
        assertEquals(1, generated.size());

        var generatedFile = outputDir.resolve(Path.of("io", "sqm", "codegen", "generated", "AnalyticsQueries.java"));
        var source = Files.readString(generatedFile);
        assertTrue(source.contains(".over(over(partition("));
    }

    @Test
    void generate_isDeterministicAcrossInputCreationOrder() throws IOException {
        var sqlDirA = tempDir.resolve("sql-a");
        var sqlDirB = tempDir.resolve("sql-b");
        var outA = tempDir.resolve("generated-a");
        var outB = tempDir.resolve("generated-b");

        writeSql(sqlDirA.resolve("user").resolve("z_list_active.sql"), "select * from users where status = :status");
        writeSql(sqlDirA.resolve("analytics").resolve("ranked.sql"), "select row_number() over (partition by dept order by salary desc) as rn from employees");
        writeSql(sqlDirA.resolve("user").resolve("a_find_by_id.sql"), "select * from users where id = :id and status = :status");

        writeSql(sqlDirB.resolve("user").resolve("a_find_by_id.sql"), "select * from users where id = :id and status = :status");
        writeSql(sqlDirB.resolve("user").resolve("z_list_active.sql"), "select * from users where status = :status");
        writeSql(sqlDirB.resolve("analytics").resolve("ranked.sql"), "select row_number() over (partition by dept order by salary desc) as rn from employees");

        var resultA = generateAndReadSources(sqlDirA, outA);
        var resultB = generateAndReadSources(sqlDirB, outB);

        assertEquals(resultA, resultB);
    }

    @Test
    void generate_isIdempotentAcrossRuns() throws IOException {
        var sqlDir = tempDir.resolve("sql-idempotent");
        var outputDir = tempDir.resolve("generated-idempotent");

        writeSql(sqlDir.resolve("reporting").resolve("kitchen_sink.sql"), """
            select u.id, count(*) as cnt
            from users u
            where u.status = :status
            group by u.id
            order by cnt desc
            """);

        var first = generateAndReadSources(sqlDir, outputDir);
        var second = generateAndReadSources(sqlDir, outputDir);

        assertEquals(first, second);
    }

    @Test
    void generate_skipsRewriteWhenSqlUnchanged() throws Exception {
        var sqlDir = tempDir.resolve("sql-cache");
        var outputDir = tempDir.resolve("generated-cache");
        var sqlFile = sqlDir.resolve("user").resolve("find_by_id.sql");
        writeSql(sqlFile, "select * from users where id = :id");

        var options = SqlFileCodegenOptions.of(sqlDir, outputDir, "io.sqm.codegen.generated");
        var generated = SqlFileCodeGenerator.of(options).generate();
        var generatedFile = generated.getFirst();
        var firstModified = Files.getLastModifiedTime(generatedFile).toMillis();
        assertTrue(Files.exists(outputDir.resolve(".sqm-codegen.hashes")));

        Thread.sleep(1200L);
        SqlFileCodeGenerator.of(options).generate();
        var secondModified = Files.getLastModifiedTime(generatedFile).toMillis();

        assertEquals(firstModified, secondModified);
    }

    @Test
    void generate_rewritesWhenSqlChanged() throws Exception {
        var sqlDir = tempDir.resolve("sql-cache-change");
        var outputDir = tempDir.resolve("generated-cache-change");
        var sqlFile = sqlDir.resolve("user").resolve("find_by_id.sql");
        writeSql(sqlFile, "select * from users where id = :id");

        var options = SqlFileCodegenOptions.of(sqlDir, outputDir, "io.sqm.codegen.generated");
        var generated = SqlFileCodeGenerator.of(options).generate();
        var generatedFile = generated.getFirst();
        var firstModified = Files.getLastModifiedTime(generatedFile).toMillis();

        Thread.sleep(1200L);
        writeSql(sqlFile, "select * from users where id = :id and status = :status");
        SqlFileCodeGenerator.of(options).generate();
        var secondModified = Files.getLastModifiedTime(generatedFile).toMillis();

        assertNotEquals(firstModified, secondModified);
    }

    @Test
    void generate_includesTimestampMetadataOnlyWhenEnabled() throws IOException {
        var sqlDir = tempDir.resolve("sql-meta");
        var outDirNoDate = tempDir.resolve("generated-meta-no-date");
        var outDirWithDate = tempDir.resolve("generated-meta-with-date");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(sqlDir.resolve("user").resolve("find_by_id.sql"), "select * from users where id = :id");

        var noDateOptions = SqlFileCodegenOptions.of(sqlDir, outDirNoDate, "io.sqm.codegen.generated", SqlCodegenDialect.ANSI, false);
        SqlFileCodeGenerator.of(noDateOptions).generate();
        var noDateSource = Files.readString(outDirNoDate.resolve(Path.of("io", "sqm", "codegen", "generated", "UserQueries.java")));
        assertTrue(noDateSource.contains("@Generated("));
        assertFalse(noDateSource.contains("date = \""));

        var withDateOptions = SqlFileCodegenOptions.of(sqlDir, outDirWithDate, "io.sqm.codegen.generated", SqlCodegenDialect.ANSI, true);
        SqlFileCodeGenerator.of(withDateOptions).generate();
        var withDateSource = Files.readString(outDirWithDate.resolve(Path.of("io", "sqm", "codegen", "generated", "UserQueries.java")));
        assertTrue(withDateSource.contains("@Generated("));
        assertTrue(withDateSource.contains("date = \""));
    }

    private Map<String, String> generateAndReadSources(Path sqlDir, Path outputDir) throws IOException {
        var options = SqlFileCodegenOptions.of(sqlDir, outputDir, "io.sqm.codegen.generated");
        var generated = SqlFileCodeGenerator.of(options).generate();
        var result = new LinkedHashMap<String, String>();
        for (var file : generated) {
            var key = outputDir.relativize(file).toString().replace('\\', '/');
            result.put(key, Files.readString(file));
        }
        return result;
    }

    private static void writeSql(Path target, String sql) throws IOException {
        Files.createDirectories(target.getParent());
        Files.writeString(target, sql);
    }
}
