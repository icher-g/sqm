package io.sqm.codegen.maven;

import io.sqm.codegen.SqlCodegenDialect;
import io.sqm.codegen.SqlFileCodeGenerator;
import io.sqm.codegen.SqlFileCodegenException;
import io.sqm.codegen.SqlFileCodegenOptions;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates Java sources from SQL files and attaches the generated directory
 * to the project's compile source roots.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class GenerateMojo extends AbstractMojo {

    /**
     * Current Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

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
            SqlFileCodegenOptions options = SqlFileCodegenOptions.of(
                Path.of(sqlDirectory),
                Path.of(generatedSourcesDirectory),
                basePackage,
                resolvedDialect,
                includeGenerationTimestamp
            );
            List<Path> generatedFiles = SqlFileCodeGenerator.of(options).generate();
            if (cleanupStaleFiles) {
                int removed = removeStaleGeneratedFiles(options, generatedFiles);
                if (removed > 0) {
                    getLog().info("SQM SQL codegen removed stale files: " + removed);
                }
            }

            project.addCompileSourceRoot(options.generatedSourcesDirectory().toString());
            getLog().info("SQM SQL codegen source root: " + options.generatedSourcesDirectory());
            getLog().info("SQM SQL codegen generated files: " + generatedFiles.size());
        } catch (SqlFileCodegenException ex) {
            throw new MojoFailureException("SQL code generation failed: " + ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw new MojoExecutionException("Failed to execute SQM SQL code generation.", ex);
        }
    }
}
