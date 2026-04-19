package io.sqm.codegen;

import io.sqm.catalog.SchemaProvider;
import io.sqm.catalog.snapshot.JsonSchemaProvider;
import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
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
        assertTrue(source.contains("public static SelectQuery aFindById()"));
        assertTrue(source.contains("return select("));
        assertTrue(source.contains("param(\"id\")"));
        assertTrue(source.contains("param(\"status\")"));
        assertTrue(source.contains(".where("));
        assertTrue(source.contains("public static Set<String> aFindByIdParams()"));
        assertTrue(source.contains("return Set.of(\"id\", \"status\")"));
        assertTrue(source.contains("public static SelectQuery zListActive()"));
        assertTrue(source.contains("param(\"status\")"));
        assertTrue(source.contains("public static Set<String> zListActiveParams()"));
        assertTrue(source.contains("return Set.of(\"status\")"));
        assertTrue(source.indexOf("public static SelectQuery aFindById()") < source.indexOf("public static SelectQuery zListActive()"));
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
    void generate_supportsMySqlDialectSpecificQueries() throws IOException {
        var sqlDir = tempDir.resolve("sql-mysql");
        var outputDir = tempDir.resolve("generated-mysql");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(
            sqlDir.resolve("user").resolve("null_safe_lookup.sql"),
            "select * from `users` where `name` <=> :name"
        );

        var options = SqlFileCodegenOptions.of(sqlDir, outputDir, "io.sqm.codegen.generated", SqlCodegenDialect.MYSQL);
        var generated = SqlFileCodeGenerator.of(options).generate();

        assertEquals(1, generated.size());
        var generatedFile = outputDir.resolve(Path.of("io", "sqm", "codegen", "generated", "UserQueries.java"));
        var source = Files.readString(generatedFile);
        assertTrue(source.contains("public static SelectQuery nullSafeLookup()"));
        assertTrue(source.contains("nullSafeEq(param(\"name\"))"));
    }

    @Test
    void generate_supportsSqlServerDialectSpecificQueries() throws IOException {
        var sqlDir = tempDir.resolve("sql-sqlserver");
        var outputDir = tempDir.resolve("generated-sqlserver");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(
            sqlDir.resolve("user").resolve("top_lookup.sql"),
            "select top (5) [u].[id], len([u].[name]) from [users] as [u] order by [u].[id]"
        );

        var options = SqlFileCodegenOptions.of(sqlDir, outputDir, "io.sqm.codegen.generated", SqlCodegenDialect.SQLSERVER);
        var generated = SqlFileCodeGenerator.of(options).generate();

        assertEquals(1, generated.size());
        var generatedFile = outputDir.resolve(Path.of("io", "sqm", "codegen", "generated", "UserQueries.java"));
        var source = Files.readString(generatedFile);
        assertTrue(source.contains("public static SelectQuery topLookup()"));
        assertTrue(source.contains(".top("));
        assertTrue(source.contains("len("));
    }

    @Test
    void generate_supportsSqlServerAdvancedQueryAndMergeFiles() throws IOException {
        var sqlDir = tempDir.resolve("sql-sqlserver-advanced");
        var outputDir = tempDir.resolve("generated-sqlserver-advanced");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(
            sqlDir.resolve("user").resolve("advanced_lookup.sql"),
            "select top (10) percent [u].[id] from [users] as [u] with (nolock) order by [u].[id]"
        );
        Files.writeString(
            sqlDir.resolve("user").resolve("sync_users.sql"),
            """
                merge top (10) percent into [users] with (holdlock)
                using [users] as [s]
                on [users].[id] = [s].[id]
                when matched and [s].[id] = 1 then update set [name] = [s].[name]
                when not matched by source and [users].[active] = 0 then delete
                output deleted.[id]
                """
        );

        var options = SqlFileCodegenOptions.of(sqlDir, outputDir, "io.sqm.codegen.generated", SqlCodegenDialect.SQLSERVER);
        var generated = SqlFileCodeGenerator.of(options).generate();

        assertEquals(1, generated.size());
        var generatedFile = outputDir.resolve(Path.of("io", "sqm", "codegen", "generated", "UserQueries.java"));
        var source = Files.readString(generatedFile);
        assertTrue(source.contains("public static SelectQuery advancedLookup()"));
        assertTrue(source.contains(".withNoLock()"));
        assertTrue(source.contains(".top("));
        assertTrue(source.contains("lit(10L)"));
        assertTrue(source.contains("public static MergeStatement syncUsers()"));
        assertTrue(source.contains(".whenNotMatchedBySourceDelete("));
        assertTrue(source.contains(".result(deleted(id(\"id\", QuoteStyle.BRACKETS)))"));
    }

    @Test
    void generate_emitsStatementMethodsForDmlFiles() throws IOException {
        var sqlDir = tempDir.resolve("sql-dml");
        var outputDir = tempDir.resolve("generated-dml");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(sqlDir.resolve("user").resolve("insert_user.sql"), "insert into users (id, name) values (1, 'alice')");
        Files.writeString(sqlDir.resolve("user").resolve("update_user.sql"), "update users set name = 'bob' where id = 1");
        Files.writeString(sqlDir.resolve("user").resolve("delete_user.sql"), "delete from users where id = 1");

        var options = SqlFileCodegenOptions.of(sqlDir, outputDir, "io.sqm.codegen.generated", SqlCodegenDialect.ANSI);
        var generated = SqlFileCodeGenerator.of(options).generate();

        assertEquals(1, generated.size());
        var generatedFile = outputDir.resolve(Path.of("io", "sqm", "codegen", "generated", "UserQueries.java"));
        var source = Files.readString(generatedFile);
        assertTrue(source.contains("public static DeleteStatement deleteUser()"));
        assertTrue(source.contains("public static InsertStatement insertUser()"));
        assertTrue(source.contains("public static UpdateStatement updateUser()"));
        assertTrue(source.contains("return delete(tbl(\"users\"))"));
        assertTrue(source.contains("return insert(tbl(\"users\"))"));
        assertTrue(source.contains("return update(tbl(\"users\"))"));
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
        assertTrue(source.contains(".over("));
        assertTrue(source.contains("partition("));
        assertTrue(source.contains("orderBy("));
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

    @Test
    void generate_returnsEmptyWhenSqlDirectoryDoesNotExist() {
        var sqlDir = tempDir.resolve("missing-sql");
        var outputDir = tempDir.resolve("generated-missing");

        var options = SqlFileCodegenOptions.of(sqlDir, outputDir, "io.sqm.codegen.generated");
        var generated = SqlFileCodeGenerator.of(options).generate();

        assertTrue(generated.isEmpty());
        assertTrue(Files.exists(outputDir.resolve(".sqm-codegen.hashes")));
    }

    @Test
    void generate_returnsEmptyWhenNoSqlFilesPresent() throws IOException {
        var sqlDir = tempDir.resolve("empty-sql");
        var outputDir = tempDir.resolve("generated-empty");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(sqlDir.resolve("user/readme.txt"), "not sql");

        var options = SqlFileCodegenOptions.of(sqlDir, outputDir, "io.sqm.codegen.generated");
        var generated = SqlFileCodeGenerator.of(options).generate();

        assertTrue(generated.isEmpty());
        assertTrue(Files.exists(outputDir.resolve(".sqm-codegen.hashes")));
    }

    @Test
    void generate_validatesAgainstJsonSchemaProviderWhenConfigured() throws Exception {
        var sqlDir = tempDir.resolve("sql-schema-valid");
        var outputDir = tempDir.resolve("generated-schema-valid");
        var schemaSnapshot = tempDir.resolve("schema-valid.json");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(sqlDir.resolve("user/find_active.sql"), "select u.id from users u where u.status = :status");

        JsonSchemaProvider.of(schemaSnapshot).save(CatalogSchema.of(
            CatalogTable.of("public", "users",
                CatalogColumn.of("id", CatalogType.LONG),
                CatalogColumn.of("status", CatalogType.STRING)
            )
        ));

        var options = SqlFileCodegenOptions.of(
            sqlDir,
            outputDir,
            "io.sqm.codegen.generated",
            SqlCodegenDialect.POSTGRESQL,
            false,
            JsonSchemaProvider.of(schemaSnapshot)
        );

        var generated = SqlFileCodeGenerator.of(options).generate();

        assertEquals(1, generated.size());
    }

    @Test
    void generate_failsWhenSchemaValidationFindsSemanticError() throws Exception {
        var sqlDir = tempDir.resolve("sql-schema-invalid");
        var outputDir = tempDir.resolve("generated-schema-invalid");
        var schemaSnapshot = tempDir.resolve("schema-invalid.json");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(sqlDir.resolve("user/find_active.sql"), "select u.missing_col from users u");

        JsonSchemaProvider.of(schemaSnapshot).save(CatalogSchema.of(
            CatalogTable.of("public", "users", CatalogColumn.of("id", CatalogType.LONG))
        ));

        var options = SqlFileCodegenOptions.of(
            sqlDir,
            outputDir,
            "io.sqm.codegen.generated",
            SqlCodegenDialect.POSTGRESQL,
            false,
            JsonSchemaProvider.of(schemaSnapshot)
        );

        var error = assertThrows(SqlFileCodegenException.class, () -> SqlFileCodeGenerator.of(options).generate());

        assertTrue(error.getMessage().contains("semantic validation failed"));
        assertTrue(error.getMessage().contains("COLUMN_NOT_FOUND"));
        assertTrue(error.getMessage().contains("user/find_active.sql"));
    }

    @Test
    void generate_collectsSemanticIssuesWhenConfiguredNotToFail() throws Exception {
        var sqlDir = tempDir.resolve("sql-schema-non-fail");
        var outputDir = tempDir.resolve("generated-schema-non-fail");
        var schemaSnapshot = tempDir.resolve("schema-non-fail.json");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(sqlDir.resolve("user/find_active.sql"), "select u.missing_col from users u");

        JsonSchemaProvider.of(schemaSnapshot).save(CatalogSchema.of(
            CatalogTable.of("public", "users", CatalogColumn.of("id", CatalogType.LONG))
        ));

        var options = SqlFileCodegenOptions.of(
            sqlDir,
            outputDir,
            "io.sqm.codegen.generated",
            SqlCodegenDialect.POSTGRESQL,
            false,
            true,
            JsonSchemaProvider.of(schemaSnapshot),
            false
        );

        var generator = SqlFileCodeGenerator.of(options);
        var generated = generator.generate();
        var issues = generator.validationIssues();

        assertEquals(1, generated.size());
        assertEquals(1, issues.size());
        assertEquals("user/find_active.sql", issues.getFirst().sqlFile().toString().replace('\\', '/'));
        assertEquals("COLUMN_NOT_FOUND", issues.getFirst().problem().code().name());
    }

    @Test
    void generate_validates_mysql_dml_against_schema_provider() throws Exception {
        var sqlDir = tempDir.resolve("sql-mysql-schema");
        var outputDir = tempDir.resolve("generated-mysql-schema");
        var schemaSnapshot = tempDir.resolve("schema-mysql.json");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(sqlDir.resolve("user").resolve("insert_user.sql"), "insert into users (id, name) values (1, 'alice')");

        JsonSchemaProvider.of(schemaSnapshot).save(CatalogSchema.of(
            CatalogTable.of("public", "users",
                CatalogColumn.of("id", CatalogType.LONG),
                CatalogColumn.of("name", CatalogType.STRING)
            )
        ));

        var options = SqlFileCodegenOptions.of(
            sqlDir,
            outputDir,
            "io.sqm.codegen.generated",
            SqlCodegenDialect.MYSQL,
            false,
            JsonSchemaProvider.of(schemaSnapshot)
        );

        var generated = SqlFileCodeGenerator.of(options).generate();

        assertEquals(1, generated.size());
        assertTrue(Files.readString(generated.getFirst()).contains("public static InsertStatement insertUser()"));
    }

    @Test
    void generate_validates_sqlserver_queries_against_schema_provider() throws Exception {
        var sqlDir = tempDir.resolve("sql-sqlserver-schema");
        var outputDir = tempDir.resolve("generated-sqlserver-schema");
        var schemaSnapshot = tempDir.resolve("schema-sqlserver.json");
        Files.createDirectories(sqlDir.resolve("user"));
        Files.writeString(
            sqlDir.resolve("user").resolve("paged_users.sql"),
            "select [u].[id] from [users] as [u] order by [u].[id] offset 2 rows fetch next 5 rows only"
        );

        JsonSchemaProvider.of(schemaSnapshot).save(CatalogSchema.of(
            CatalogTable.of("public", "users", CatalogColumn.of("id", CatalogType.LONG))
        ));

        var options = SqlFileCodegenOptions.of(
            sqlDir,
            outputDir,
            "io.sqm.codegen.generated",
            SqlCodegenDialect.SQLSERVER,
            false,
            JsonSchemaProvider.of(schemaSnapshot)
        );

        var generated = SqlFileCodeGenerator.of(options).generate();

        assertEquals(1, generated.size());
        assertTrue(Files.readString(generated.getFirst()).contains("public static SelectQuery pagedUsers()"));
    }

    @Test
    void generate_wraps_schema_provider_sql_exception() throws IOException {
        var sqlDir = tempDir.resolve("sql-schema-provider-error");
        var outputDir = tempDir.resolve("generated-schema-provider-error");
        writeSql(sqlDir.resolve("user").resolve("find_by_id.sql"), "select * from users where id = :id");

        SchemaProvider failingProvider = () -> {
            throw new SQLException("boom");
        };

        var options = SqlFileCodegenOptions.of(
            sqlDir,
            outputDir,
            "io.sqm.codegen.generated",
            SqlCodegenDialect.MYSQL,
            false,
            failingProvider
        );

        var error = assertThrows(SqlFileCodegenException.class, () -> SqlFileCodeGenerator.of(options));

        assertTrue(error.getMessage().contains("Failed to load schema for validation"));
        assertTrue(error.getMessage().contains("boom"));
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
