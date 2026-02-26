package io.sqm.codegen.maven;

import io.sqm.codegen.*;
import io.sqm.catalog.SchemaProvider;
import io.sqm.catalog.jdbc.JdbcSchemaProvider;
import io.sqm.catalog.snapshot.JsonSchemaProvider;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates Java sources from SQL files and attaches the generated directory
 * to the project's compile source roots.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class GenerateMojo extends AbstractMojo {

    /**
     * Creates the SQL code generation Mojo.
     */
    public GenerateMojo() {
    }

    /**
     * Current Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;
    /**
     * Maven settings used to resolve credentials from server entries.
     */
    @Parameter(defaultValue = "${settings}", readonly = true)
    private Settings settings;

    /**
     * Skip SQL code generation.
     */
    @Parameter(property = "sqm.codegen.skip", defaultValue = "false")
    private boolean skip;

    /**
     * SQL dialect used for parse validation.
     */
    @Parameter(property = "sqm.codegen.dialect", defaultValue = "ansi")
    private String dialect;

    /**
     * Base package name for generated classes.
     */
    @Parameter(property = "sqm.codegen.basePackage", defaultValue = "io.sqm.codegen.generated")
    private String basePackage;

    /**
     * Directory that contains {@code *.sql} files.
     */
    @Parameter(property = "sqm.codegen.sqlDirectory", defaultValue = "${project.basedir}/src/main/sql")
    private String sqlDirectory;

    /**
     * Output directory for generated Java files.
     */
    @Parameter(property = "sqm.codegen.generatedSourcesDirectory", defaultValue = "${project.build.directory}/generated-sources/sqm-codegen")
    private String generatedSourcesDirectory;

    /**
     * Removes stale generated Java files under the configured package directory
     * that were not produced in the current run.
     */
    @Parameter(property = "sqm.codegen.cleanupStaleFiles", defaultValue = "true")
    private boolean cleanupStaleFiles;

    /**
     * Adds generation timestamp to {@code @Generated} metadata in produced classes.
     */
    @Parameter(property = "sqm.codegen.includeGenerationTimestamp", defaultValue = "false")
    private boolean includeGenerationTimestamp;

    /**
     * Schema provider kind used for semantic validation before code emission.
     * Supported values: {@code none}, {@code json}, {@code jdbc}.
     */
    @Parameter(property = "sqm.codegen.schemaProvider", defaultValue = "none")
    private String schemaProvider;

    /**
     * JSON schema snapshot path used when {@code sqm.codegen.schemaProvider=json}.
     */
    @Parameter(property = "sqm.codegen.schemaSnapshotPath")
    private String schemaSnapshotPath;

    /**
     * JDBC URL used when {@code sqm.codegen.schemaProvider=jdbc}.
     */
    @Parameter(property = "sqm.codegen.schemaJdbcUrl")
    private String schemaJdbcUrl;

    /**
     * JDBC username used when {@code sqm.codegen.schemaProvider=jdbc}.
     */
    @Parameter(property = "sqm.codegen.schemaJdbcUsername")
    private String schemaJdbcUsername;

    /**
     * JDBC password used when {@code sqm.codegen.schemaProvider=jdbc}.
     */
    @Parameter(property = "sqm.codegen.schemaJdbcPassword")
    private String schemaJdbcPassword;
    /**
     * Optional Maven settings server id used to resolve JDBC credentials.
     */
    @Parameter(property = "sqm.codegen.schemaJdbcServerId")
    private String schemaJdbcServerId;
    /**
     * Environment variable name for JDBC username fallback.
     */
    @Parameter(property = "sqm.codegen.schemaJdbcUsernameEnv", defaultValue = "SQM_SCHEMA_JDBC_USERNAME")
    private String schemaJdbcUsernameEnv;
    /**
     * Environment variable name for JDBC password fallback.
     */
    @Parameter(property = "sqm.codegen.schemaJdbcPasswordEnv", defaultValue = "SQM_SCHEMA_JDBC_PASSWORD")
    private String schemaJdbcPasswordEnv;

    /**
     * Optional JDBC catalog filter used when {@code sqm.codegen.schemaProvider=jdbc}.
     */
    @Parameter(property = "sqm.codegen.schemaJdbcCatalog")
    private String schemaJdbcCatalog;

    /**
     * Optional JDBC schema pattern filter used when {@code sqm.codegen.schemaProvider=jdbc}.
     */
    @Parameter(property = "sqm.codegen.schemaJdbcSchemaPattern")
    private String schemaJdbcSchemaPattern;
    /**
     * Local schema cache path reused between codegen runs.
     */
    @Parameter(property = "sqm.codegen.schemaCachePath", defaultValue = "${project.build.directory}/sqm-codegen/schema-cache.json")
    private String schemaCachePath;
    /**
     * Forces JDBC refresh even when schema cache exists.
     */
    @Parameter(property = "sqm.codegen.schemaCacheRefresh", defaultValue = "false")
    private boolean schemaCacheRefresh;
    /**
     * Writes introspected schema into local schema cache file.
     */
    @Parameter(property = "sqm.codegen.schemaCacheWrite", defaultValue = "true")
    private boolean schemaCacheWrite;
    /**
     * Maximum cache age in minutes. {@code <= 0} disables expiration checks.
     */
    @Parameter(property = "sqm.codegen.schemaCacheTtlMinutes", defaultValue = "0")
    private long schemaCacheTtlMinutes;
    /**
     * Comma-separated regex patterns of schema names to include.
     */
    @Parameter(property = "sqm.codegen.schemaIncludePatterns")
    private String schemaIncludePatterns;
    /**
     * Comma-separated regex patterns of schema names to exclude.
     */
    @Parameter(property = "sqm.codegen.schemaExcludePatterns")
    private String schemaExcludePatterns;
    /**
     * Comma-separated regex patterns of table names to include.
     */
    @Parameter(property = "sqm.codegen.tableIncludePatterns")
    private String tableIncludePatterns;
    /**
     * Comma-separated regex patterns of table names to exclude.
     */
    @Parameter(property = "sqm.codegen.tableExcludePatterns")
    private String tableExcludePatterns;
    /**
     * Expected database product name pinned to cache metadata, for example {@code PostgreSQL}.
     */
    @Parameter(property = "sqm.codegen.schemaCacheExpectedDatabaseProduct")
    private String schemaCacheExpectedDatabaseProduct;
    /**
     * Expected database major version pinned to cache metadata.
     */
    @Parameter(property = "sqm.codegen.schemaCacheExpectedDatabaseMajorVersion")
    private Integer schemaCacheExpectedDatabaseMajorVersion;
    /**
     * Whether semantic validation failures should fail code generation.
     */
    @Parameter(property = "sqm.codegen.failOnValidationError", defaultValue = "true")
    private boolean failOnValidationError = true;
    /**
     * Optional validation report JSON output path.
     */
    @Parameter(property = "sqm.codegen.validationReportPath", defaultValue = "${project.build.directory}/sqm-codegen/validation-report.json")
    private String validationReportPath;

    static int removeStaleGeneratedFiles(SqlFileCodegenOptions options, java.util.List<Path> generatedFiles) {
        Path packageDir = options.generatedSourcesDirectory().resolve(options.basePackage().replace('.', '/'));
        if (!Files.exists(packageDir)) {
            return 0;
        }

        Set<Path> expected = generatedFiles.stream()
            .map(Path::normalize)
            .collect(Collectors.toCollection(HashSet::new));

        int removedCount = 0;
        try (Stream<Path> stream = Files.walk(packageDir)) {
            List<Path> existingGeneratedFiles = stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".java"))
                .map(Path::normalize)
                .toList();

            for (Path existing : existingGeneratedFiles) {
                if (!expected.contains(existing)) {
                    Files.deleteIfExists(existing);
                    removedCount++;
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to remove stale generated files from " + packageDir, ex);
        }

        try (Stream<Path> stream = Files.walk(packageDir)) {
            List<Path> dirs = stream
                .filter(Files::isDirectory)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toCollection(ArrayList::new));
            for (Path dir : dirs) {
                if (!dir.equals(packageDir) && isDirectoryEmpty(dir)) {
                    Files.deleteIfExists(dir);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to clean empty generated directories under " + packageDir, ex);
        }

        return removedCount;
    }

    private static boolean isDirectoryEmpty(Path dir) throws IOException {
        try (Stream<Path> children = Files.list(dir)) {
            return children.findFirst().isEmpty();
        }
    }

    private static String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String firstNonBlank(String... candidates) {
        for (var candidate : candidates) {
            var value = normalizeBlank(candidate);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static List<Pattern> parseRegexList(String value) {
        var normalized = normalizeBlank(value);
        if (normalized == null) {
            return List.of();
        }
        return Arrays.stream(normalized.split(","))
            .map(String::trim)
            .filter(part -> !part.isEmpty())
            .map(Pattern::compile)
            .toList();
    }

    /**
     * Executes SQL file code generation.
     *
     * @throws MojoExecutionException when generation cannot be executed.
     * @throws MojoFailureException   when SQL parsing/code generation fails for input files.
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping SQM SQL code generation (sqm.codegen.skip=true).");
            return;
        }

        try {
            SqlCodegenDialect resolvedDialect = SqlCodegenDialect.from(dialect);
            SchemaProvider resolvedSchemaProvider = resolveSchemaProvider();
            SqlFileCodegenOptions options = SqlFileCodegenOptions.of(
                Path.of(sqlDirectory),
                Path.of(generatedSourcesDirectory),
                basePackage,
                resolvedDialect,
                includeGenerationTimestamp,
                resolvedSchemaProvider,
                failOnValidationError
            );
            var generator = SqlFileCodeGenerator.of(options);
            List<Path> generatedFiles = generator.generate();
            if (cleanupStaleFiles) {
                int removed = removeStaleGeneratedFiles(options, generatedFiles);
                if (removed > 0) {
                    getLog().info("SQM SQL codegen removed stale files: " + removed);
                }
            }
            writeValidationReport(generator.validationIssues());

            project.addCompileSourceRoot(options.generatedSourcesDirectory().toString());
            getLog().info("SQM SQL codegen source root: " + options.generatedSourcesDirectory());
            getLog().info("SQM SQL codegen generated files: " + generatedFiles.size());
        } catch (SqlFileCodegenException ex) {
            throw new MojoFailureException("SQL code generation failed: " + ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw new MojoExecutionException("Failed to execute SQM SQL code generation.", ex);
        }
    }

    private SchemaProvider resolveSchemaProvider() {
        var kind = (schemaProvider == null ? "none" : schemaProvider.trim().toLowerCase(Locale.ROOT));
        return switch (kind) {
            case "", "none" -> null;
            case "json" -> resolveJsonSchemaProvider();
            case "jdbc" -> resolveJdbcSchemaProvider();
            default -> throw new IllegalArgumentException(
                "Unsupported schema provider: " + schemaProvider + ". Supported values: none, json, jdbc"
            );
        };
    }

    private SchemaProvider resolveJsonSchemaProvider() {
        if (schemaSnapshotPath == null || schemaSnapshotPath.isBlank()) {
            throw new IllegalArgumentException(
                "sqm.codegen.schemaSnapshotPath is required when sqm.codegen.schemaProvider=json"
            );
        }
        return JsonSchemaProvider.of(Path.of(schemaSnapshotPath));
    }

    private SchemaProvider resolveJdbcSchemaProvider() {
        if (schemaJdbcUrl == null || schemaJdbcUrl.isBlank()) {
            throw new IllegalArgumentException(
                "sqm.codegen.schemaJdbcUrl is required when sqm.codegen.schemaProvider=jdbc"
            );
        }
        var credentials = resolveJdbcCredentials();
        var dataSource = new JdbcDriverManagerDataSource(schemaJdbcUrl, credentials.username(), credentials.password());
        var jdbcProvider = JdbcSchemaProvider.of(
            dataSource,
            normalizeBlank(schemaJdbcCatalog),
            normalizeBlank(schemaJdbcSchemaPattern),
            List.of("TABLE", "VIEW", "MATERIALIZED VIEW", "FOREIGN TABLE"),
            io.sqm.catalog.postgresql.PostgresSqlTypeMapper.standard()
        );
        var filteredProvider = filteredProvider(jdbcProvider);
        var cachePath = resolveSchemaCachePath();
        if (cachePath == null) {
            return filteredProvider;
        }
        return new CachingSchemaProvider(
            filteredProvider,
            dataSource,
            cachePath,
            schemaCacheRefresh,
            schemaCacheWrite,
            schemaCacheTtlMinutes,
            expectedCacheMetadata(),
            getLog()
        );
    }

    private SchemaProvider filteredProvider(SchemaProvider delegate) {
        var schemaIncludes = parseRegexList(schemaIncludePatterns);
        var schemaExcludes = parseRegexList(schemaExcludePatterns);
        var tableIncludes = parseRegexList(tableIncludePatterns);
        var tableExcludes = parseRegexList(tableExcludePatterns);
        if (schemaIncludes.isEmpty() && schemaExcludes.isEmpty() && tableIncludes.isEmpty() && tableExcludes.isEmpty()) {
            return delegate;
        }
        return new RegexFilteringSchemaProvider(delegate, schemaIncludes, schemaExcludes, tableIncludes, tableExcludes);
    }

    private Path resolveSchemaCachePath() {
        var value = normalizeBlank(schemaCachePath);
        return value == null ? null : Path.of(value);
    }

    private JdbcCredentials resolveJdbcCredentials() {
        var username = firstNonBlank(
            schemaJdbcUsername,
            readEnv(schemaJdbcUsernameEnv),
            readFromSettingsServer(Server::getUsername)
        );
        var password = firstNonBlank(
            schemaJdbcPassword,
            readEnv(schemaJdbcPasswordEnv),
            readFromSettingsServer(Server::getPassword)
        );
        return new JdbcCredentials(username, password);
    }

    private String readEnv(String envVarName) {
        var name = normalizeBlank(envVarName);
        if (name == null) {
            return null;
        }
        return normalizeBlank(System.getenv(name));
    }

    private String readFromSettingsServer(java.util.function.Function<Server, String> extractor) {
        var id = normalizeBlank(schemaJdbcServerId);
        if (id == null || settings == null) {
            return null;
        }
        var server = settings.getServer(id);
        if (server == null) {
            throw new IllegalArgumentException("Maven settings server not found: " + id);
        }
        return normalizeBlank(extractor.apply(server));
    }

    private SchemaCacheMetadata expectedCacheMetadata() {
        return new SchemaCacheMetadata(
            SqlCodegenDialect.from(dialect).name(),
            normalizeBlank(schemaCacheExpectedDatabaseProduct),
            schemaCacheExpectedDatabaseMajorVersion,
            0L
        );
    }

    private void writeValidationReport(List<SqlValidationIssue> issues) {
        var reportPath = normalizeBlank(validationReportPath);
        if (reportPath == null || reportPath.contains("${")) {
            return;
        }
        try {
            var jsonPath = Path.of(reportPath);
            new ValidationReportWriter(jsonPath, failOnValidationError).write(issues);
            getLog().info("SQM SQL codegen validation report: " + jsonPath);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write SQM SQL validation report", ex);
        }
    }

}
