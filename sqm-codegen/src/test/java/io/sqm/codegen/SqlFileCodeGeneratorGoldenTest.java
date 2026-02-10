package io.sqm.codegen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlFileCodeGeneratorGoldenTest {

    @TempDir
    Path tempDir;

    private static String normalize(String value) {
        return value.replace("\r\n", "\n");
    }

    @Test
    void generate_matchesGoldenJavaFiles() throws IOException {
        var sqlDir = tempDir.resolve("sql");
        var outDir = tempDir.resolve("generated");

        writeResource("golden/sql/user/a_find_by_id.sql", sqlDir.resolve("user/a_find_by_id.sql"));
        writeResource("golden/sql/user/z_list_active.sql", sqlDir.resolve("user/z_list_active.sql"));
        writeResource("golden/sql/analytics/ranked.sql", sqlDir.resolve("analytics/ranked.sql"));
        writeResource("golden/sql/reporting/kitchen_sink.sql", sqlDir.resolve("reporting/kitchen_sink.sql"));

        var options = SqlFileCodegenOptions.of(sqlDir, outDir, "io.sqm.codegen.generated");
        var generated = SqlFileCodeGenerator.of(options).generate();
        assertEquals(3, generated.size());

        assertGolden(outDir.resolve("io/sqm/codegen/generated/UserQueries.java"), "golden/expected/UserQueries.java");
        assertGolden(outDir.resolve("io/sqm/codegen/generated/AnalyticsQueries.java"), "golden/expected/AnalyticsQueries.java");
        assertGolden(outDir.resolve("io/sqm/codegen/generated/ReportingQueries.java"), "golden/expected/ReportingQueries.java");
    }

    private void assertGolden(Path actualFile, String expectedResource) throws IOException {
        var actual = normalize(Files.readString(actualFile, StandardCharsets.UTF_8));
        var expected = normalize(readResource(expectedResource));
        assertEquals(expected, actual, "Golden mismatch for " + actualFile.getFileName());
    }

    private void writeResource(String resourceName, Path target) throws IOException {
        var content = readResource(resourceName);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }

    private String readResource(String resourceName) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IOException("Missing test resource: " + resourceName);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
