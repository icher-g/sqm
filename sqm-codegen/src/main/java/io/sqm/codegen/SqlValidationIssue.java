package io.sqm.codegen;

import io.sqm.validate.api.ValidationProblem;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Semantic validation issue collected during SQL file code generation.
 *
 * @param sqlFile SQL file path relative to configured SQL root.
 * @param problem semantic validation problem.
 */
public record SqlValidationIssue(Path sqlFile, ValidationProblem problem) {
    /**
     * Creates a semantic validation issue.
     *
     * @param sqlFile SQL file path relative to configured SQL root.
     * @param problem semantic validation problem.
     */
    public SqlValidationIssue {
        Objects.requireNonNull(sqlFile, "sqlFile");
        Objects.requireNonNull(problem, "problem");
    }
}

