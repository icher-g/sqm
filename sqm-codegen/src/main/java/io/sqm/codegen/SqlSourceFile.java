package io.sqm.codegen;

import io.sqm.core.Statement;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents one parsed SQL file ready for Java DSL emission.
 *
 * @param relativePath SQL file path relative to the configured source root
 * @param folder       folder that owns the SQL file
 * @param methodName   generated Java method name
 * @param parameters   named parameters referenced by the statement
 * @param sqlHash      hash of the SQL source content used for incremental generation
 * @param statements   parsed statements in source order
 */
public record SqlSourceFile(
    Path relativePath,
    Path folder,
    String methodName,
    Set<String> parameters,
    String sqlHash,
    List<Statement> statements
) {

    /**
     * Creates a SQL source file.
     *
     * @param relativePath SQL file path relative to the configured source root
     * @param folder       folder that owns the SQL file
     * @param methodName   generated Java method name
     * @param parameters   named parameters referenced by the statement
     * @param sqlHash      hash of the SQL source content used for incremental generation
     * @param statements   parsed statements in source order
     */
    public SqlSourceFile {
        Objects.requireNonNull(relativePath, "relativePath");
        Objects.requireNonNull(folder, "folder");
        Objects.requireNonNull(methodName, "methodName");
        Objects.requireNonNull(parameters, "parameters");
        Objects.requireNonNull(sqlHash, "sqlHash");
        Objects.requireNonNull(statements, "statements");
        parameters = Set.copyOf(parameters);
        statements = List.copyOf(statements);
        if (statements.isEmpty()) {
            throw new IllegalArgumentException("statements cannot be empty");
        }
    }
}
