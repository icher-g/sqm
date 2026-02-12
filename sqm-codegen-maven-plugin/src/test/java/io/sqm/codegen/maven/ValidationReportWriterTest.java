package io.sqm.codegen.maven;

import io.sqm.codegen.SqlValidationIssue;
import io.sqm.validate.api.ValidationProblem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationReportWriterTest {
    @TempDir
    Path tempDir;

    @Test
    void writesVersionedJsonAndSummaryWithEscaping() throws Exception {
        var reportPath = tempDir.resolve("reports/validation.json");
        var writer = new ValidationReportWriter(reportPath, false);
        var issue = new SqlValidationIssue(
            Path.of("user/find_by_id.sql"),
            new ValidationProblem(
                ValidationProblem.Code.COLUMN_NOT_FOUND,
                "Missing column \"name\"\nline2",
                "column",
                "select.where"
            )
        );

        writer.write(List.of(issue));

        var json = Files.readString(reportPath, StandardCharsets.UTF_8);
        var summary = Files.readString(Path.of(reportPath + ".txt"), StandardCharsets.UTF_8);

        assertTrue(json.contains("\"formatVersion\": 1"));
        assertTrue(json.contains("\"failOnValidationError\": false"));
        assertTrue(json.contains("\"issuesCount\": 1"));
        assertTrue(json.contains("\"COLUMN_NOT_FOUND\""));
        assertTrue(json.contains("Missing column \\\"name\\\"\\nline2"));
        assertTrue(json.contains("\"clausePath\": \"select.where\""));
        assertTrue(json.contains("\"nodeKind\": \"column\""));

        assertTrue(summary.contains("failOnValidationError=false"));
        assertTrue(summary.contains("issuesCount=1"));
        assertTrue(summary.contains("user/find_by_id.sql: COLUMN_NOT_FOUND: Missing column \"name\""));
    }

    @Test
    void writesNullOptionalFieldsInJson() throws Exception {
        var reportPath = tempDir.resolve("reports/validation-nullables.json");
        var writer = new ValidationReportWriter(reportPath, true);
        var issue = new SqlValidationIssue(
            Path.of("user/list.sql"),
            new ValidationProblem(ValidationProblem.Code.TABLE_NOT_FOUND, "Missing table")
        );

        writer.write(List.of(issue));

        var json = Files.readString(reportPath, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"formatVersion\": 1"));
        assertTrue(json.contains("\"failOnValidationError\": true"));
        assertTrue(json.contains("\"clausePath\": null"));
        assertTrue(json.contains("\"nodeKind\": null"));
    }
}

