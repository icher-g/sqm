package io.sqm.codegen;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Configuration for SQL file code generation.
 */
public final class SqlFileCodegenOptions {
    private final Path sqlDirectory;
    private final Path generatedSourcesDirectory;
    private final String basePackage;
    private final SqlCodegenDialect dialect;
    private final boolean includeGenerationTimestamp;

    private SqlFileCodegenOptions(
        Path sqlDirectory,
        Path generatedSourcesDirectory,
        String basePackage,
        SqlCodegenDialect dialect,
        boolean includeGenerationTimestamp
    ) {
        this.sqlDirectory = sqlDirectory;
        this.generatedSourcesDirectory = generatedSourcesDirectory;
        this.basePackage = basePackage;
        this.dialect = dialect;
        this.includeGenerationTimestamp = includeGenerationTimestamp;
    }

    /**
     * Creates options for SQL file code generation.
     *
     * @param sqlDirectory source directory that contains {@code *.sql} files.
     * @param generatedSourcesDirectory output directory where generated Java files are written.
     * @param basePackage Java package for generated classes.
     * @return new immutable options.
     */
    public static SqlFileCodegenOptions of(Path sqlDirectory, Path generatedSourcesDirectory, String basePackage) {
        return of(sqlDirectory, generatedSourcesDirectory, basePackage, SqlCodegenDialect.ANSI, false);
    }

    /**
     * Creates options for SQL file code generation.
     *
     * @param sqlDirectory source directory that contains {@code *.sql} files.
     * @param generatedSourcesDirectory output directory where generated Java files are written.
     * @param basePackage Java package for generated classes.
     * @param dialect SQL dialect used for parse validation.
     * @return new immutable options.
     */
    public static SqlFileCodegenOptions of(
        Path sqlDirectory,
        Path generatedSourcesDirectory,
        String basePackage,
        SqlCodegenDialect dialect
    ) {
        return of(sqlDirectory, generatedSourcesDirectory, basePackage, dialect, false);
    }

    /**
     * Creates options for SQL file code generation.
     *
     * @param sqlDirectory source directory that contains {@code *.sql} files.
     * @param generatedSourcesDirectory output directory where generated Java files are written.
     * @param basePackage Java package for generated classes.
     * @param dialect SQL dialect used for parse validation.
     * @param includeGenerationTimestamp if {@code true}, generated classes include {@code @Generated(date=...)} metadata.
     * @return new immutable options.
     */
    public static SqlFileCodegenOptions of(
        Path sqlDirectory,
        Path generatedSourcesDirectory,
        String basePackage,
        SqlCodegenDialect dialect,
        boolean includeGenerationTimestamp
    ) {
        Objects.requireNonNull(sqlDirectory, "sqlDirectory");
        Objects.requireNonNull(generatedSourcesDirectory, "generatedSourcesDirectory");
        Objects.requireNonNull(basePackage, "basePackage");
        Objects.requireNonNull(dialect, "dialect");
        var normalizedPackage = basePackage.trim();
        if (normalizedPackage.isEmpty()) {
            throw new IllegalArgumentException("basePackage must not be blank");
        }
        return new SqlFileCodegenOptions(
            sqlDirectory.normalize(),
            generatedSourcesDirectory.normalize(),
            normalizedPackage,
            dialect,
            includeGenerationTimestamp
        );
    }

    /**
     * Returns the source SQL directory.
     *
     * @return source SQL directory.
     */
    public Path sqlDirectory() {
        return sqlDirectory;
    }

    /**
     * Returns the output directory for generated Java sources.
     *
     * @return generated Java sources directory.
     */
    public Path generatedSourcesDirectory() {
        return generatedSourcesDirectory;
    }

    /**
     * Returns the base package for generated classes.
     *
     * @return base package name.
     */
    public String basePackage() {
        return basePackage;
    }

    /**
     * Returns the SQL dialect used for parse validation.
     *
     * @return SQL dialect.
     */
    public SqlCodegenDialect dialect() {
        return dialect;
    }

    /**
     * Returns whether generated classes should include generation timestamp metadata.
     *
     * @return {@code true} when timestamp metadata should be included.
     */
    public boolean includeGenerationTimestamp() {
        return includeGenerationTimestamp;
    }
}
