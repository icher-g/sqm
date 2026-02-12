package io.sqm.codegen.maven;

import io.sqm.codegen.SqlValidationIssue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Writes semantic validation reports produced during SQL code generation.
 * The writer emits a JSON report and a plain text summary file.
 */
final class ValidationReportWriter {
    private final Path jsonPath;
    private final boolean failOnValidationError;

    /**
     * Creates report writer.
     *
     * @param jsonPath JSON report output path.
     * @param failOnValidationError current fail policy used by code generation.
     */
    ValidationReportWriter(Path jsonPath, boolean failOnValidationError) {
        this.jsonPath = Objects.requireNonNull(jsonPath, "jsonPath");
        this.failOnValidationError = failOnValidationError;
    }

    /**
     * Writes validation reports.
     *
     * @param issues collected semantic validation issues.
     * @throws IOException when report writing fails.
     */
    void write(List<SqlValidationIssue> issues) throws IOException {
        var summaryPath = Path.of(jsonPath + ".txt");
        var parent = jsonPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        var json = new StringBuilder(1024);
        json.append("{\n");
        json.append("  \"formatVersion\": 1,\n");
        json.append("  \"failOnValidationError\": ").append(failOnValidationError).append(",\n");
        json.append("  \"issuesCount\": ").append(issues.size()).append(",\n");
        json.append("  \"issues\": [\n");
        for (int i = 0; i < issues.size(); i++) {
            var issue = issues.get(i);
            var problem = issue.problem();
            json.append("    {\n");
            json.append("      \"sqlFile\": \"").append(escapeJson(issue.sqlFile().toString().replace('\\', '/'))).append("\",\n");
            json.append("      \"code\": \"").append(problem.code().name()).append("\",\n");
            json.append("      \"message\": \"").append(escapeJson(problem.message())).append("\",\n");
            json.append("      \"clausePath\": ").append(toJsonString(problem.clausePath())).append(",\n");
            json.append("      \"nodeKind\": ").append(toJsonString(problem.nodeKind())).append("\n");
            json.append("    }");
            if (i + 1 < issues.size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append("  ]\n");
        json.append("}\n");
        Files.writeString(jsonPath, json.toString());

        var summary = new StringBuilder(512);
        summary.append("SQM SQL validation report").append('\n');
        summary.append("failOnValidationError=").append(failOnValidationError).append('\n');
        summary.append("issuesCount=").append(issues.size()).append('\n');
        for (var issue : issues) {
            var problem = issue.problem();
            summary.append("- ")
                .append(issue.sqlFile().toString().replace('\\', '/'))
                .append(": ")
                .append(problem.code().name())
                .append(": ")
                .append(problem.message())
                .append('\n');
        }
        Files.writeString(summaryPath, summary.toString());
    }

    private static String toJsonString(String value) {
        var normalized = value == null || value.isBlank() ? null : value;
        return normalized == null ? "null" : "\"" + escapeJson(normalized) + "\"";
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n")
            .replace("\t", "\\t");
    }
}
