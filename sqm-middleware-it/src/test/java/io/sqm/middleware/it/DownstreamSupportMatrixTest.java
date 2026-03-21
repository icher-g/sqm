package io.sqm.middleware.it;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownstreamSupportMatrixTest {

    private static final Path MATRIX_PATH = Path.of("..", "docs", "downstream", "DOWNSTREAM_SUPPORT_MATRIX.md");
    private static final List<String> SUPPORTED_STATUS_VALUES = List.of("Yes", "No");

    @Test
    void downstream_support_matrix_exists_and_declares_expected_module_rows() throws IOException {
        assertTrue(Files.exists(MATRIX_PATH), "Expected downstream support matrix doc to exist");

        Map<String, List<String>> rows = matrixRows();

        assertRow(rows, "sqm-validate", "Yes", "Yes", "Yes", "Yes", "Yes", "Yes", "Yes", "Yes");
        assertRow(rows, "sqm-control", "Yes", "Yes", "Yes", "Yes", "Yes", "Yes", "Yes", "Yes");
        assertRow(rows, "sqm-codegen", "Yes", "Yes", "Yes", "Yes", "Yes", "Yes", "Yes", "Yes");
        assertRow(rows, "sqm-codegen-maven-plugin", "Yes", "Yes", "Yes", "Yes", "Yes", "Yes", "Yes", "Yes");
        assertRow(rows, "sqm-middleware-core", "Yes", "Yes", "Yes", "Yes", "Yes", "Yes", "Yes", "Yes");
        assertRow(rows, "sqm-middleware-rest", "Yes", "Yes", "Yes", "Yes", "Yes", "Yes", "Yes", "Yes");
        assertRow(rows, "sqm-middleware-mcp", "Yes", "Yes", "Yes", "Yes", "Yes", "Yes", "Yes", "Yes");
    }

    @Test
    void downstream_support_matrix_uses_only_supported_status_values() throws IOException {
        Map<String, List<String>> rows = matrixRows();

        assertFalse(rows.isEmpty(), "Expected downstream support matrix to declare at least one module row");
        for (Map.Entry<String, List<String>> row : rows.entrySet()) {
            for (int i = 0; i < 8; i++) {
                String actualValue = row.getValue().get(i);
                assertTrue(
                    SUPPORTED_STATUS_VALUES.contains(actualValue),
                    "Unexpected support value for module '%s' at column %d: %s"
                        .formatted(row.getKey(), i, actualValue)
                );
            }
        }
    }

    private static void assertRow(
        Map<String, List<String>> rows,
        String module,
        String ansiQuery,
        String ansiDml,
        String postgresQuery,
        String postgresDml,
        String mysqlQuery,
        String mysqlDml,
        String sqlServerQuery,
        String sqlServerDml
    ) {
        assertTrue(rows.containsKey(module), "Missing downstream support matrix row for module: " + module);
        assertEquals(
            List.of(ansiQuery, ansiDml, postgresQuery, postgresDml, mysqlQuery, mysqlDml, sqlServerQuery, sqlServerDml),
            rows.get(module).subList(0, 8),
            "Unexpected support matrix values for module: " + module
        );
    }

    private static Map<String, List<String>> matrixRows() throws IOException {
        Map<String, List<String>> rows = new LinkedHashMap<>();
        for (String line : Files.readAllLines(MATRIX_PATH)) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("| `sqm-")) {
                continue;
            }

            String[] cells = trimmed.substring(1, trimmed.length() - 1).split("\\|", -1);
            for (int i = 0; i < cells.length; i++) {
                cells[i] = cells[i].trim();
            }

            rows.put(
                cells[0].replace("`", ""),
                List.of(cells[1], cells[2], cells[3], cells[4], cells[5], cells[6], cells[7], cells[8])
            );
        }
        return rows;
    }
}
