package io.sqm.codegen;

import io.sqm.core.Statement;

import java.nio.file.Path;
import java.util.Set;

/**
 * Represents one parsed SQL file ready for Java DSL emission.
 *
 * @param relativePath SQL file path relative to the configured source root
 * @param folder folder that owns the SQL file
 * @param methodName generated Java method name
 * @param statement parsed SQM statement model
 * @param parameters named parameters referenced by the statement
 * @param sqlHash hash of the SQL source content used for incremental generation
 */
public record SqlSourceFile(Path relativePath, Path folder, String methodName, Statement statement, Set<String> parameters, String sqlHash) {
}
