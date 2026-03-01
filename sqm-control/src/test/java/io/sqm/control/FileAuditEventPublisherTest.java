package io.sqm.control;

import io.sqm.control.audit.FileAuditEventPublisher;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class FileAuditEventPublisherTest {

    @Test
    void appends_json_line_to_file() throws Exception {
        var output = Files.createTempFile("sqm-audit", ".log");
        try {
            var publisher = FileAuditEventPublisher.of(output);
            var event = new AuditEvent(
                "select 1",
                "select 1",
                java.util.List.of(ReasonCode.NONE),
                null,
                DecisionResult.allow(),
                ExecutionContext.of("postgresql", "alice", "tenant-a", ExecutionMode.ANALYZE, ParameterizationMode.OFF),
                123L
            );

            publisher.publish(event);

            var content = Files.readString(output);
            assertTrue(content.contains("\"decisionKind\":\"ALLOW\""));
            assertTrue(content.contains("\"dialect\":\"postgresql\""));
            assertTrue(content.contains("\"principal\":\"alice\""));
        } finally {
            Files.deleteIfExists(output);
        }
    }

    @Test
    void rejects_null_path() {
        assertThrows(NullPointerException.class, () -> FileAuditEventPublisher.of(null));
    }

    @Test
    void rejects_null_event() throws Exception {
        var output = Files.createTempFile("sqm-audit", ".log");
        try {
            var publisher = FileAuditEventPublisher.of(output);
            assertThrows(NullPointerException.class, () -> publisher.publish(null));
        } finally {
            Files.deleteIfExists(output);
        }
    }

    @Test
    void creates_parent_directories_and_escapes_json_values() throws Exception {
        var dir = Files.createTempDirectory("sqm-audit-dir");
        var output = dir.resolve("nested").resolve("audit.log");
        try {
            var publisher = FileAuditEventPublisher.of(output);
            var event = new AuditEvent(
                "select \"x\"\nfrom t",
                "select \"x\"\nfrom t",
                java.util.List.of(ReasonCode.NONE),
                null,
                DecisionResult.allow(),
                ExecutionContext.of("postgresql", "a\"b", "tenant\t1", ExecutionMode.ANALYZE, ParameterizationMode.OFF),
                1L
            );

            publisher.publish(event);
            var content = Files.readString(output);
            assertTrue(content.contains("\\\"x\\\""));
            assertTrue(content.contains("\\nfrom t"));
            assertTrue(content.contains("a\\\"b"));
            assertTrue(content.contains("tenant\\t1"));
            assertTrue(content.contains("\"fingerprint\":\"\""));
        } finally {
            Files.walk(dir)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                    }
                });
        }
    }

    @Test
    void rotates_when_max_bytes_is_exceeded_and_retains_history() throws Exception {
        var dir = Files.createTempDirectory("sqm-audit-rotate");
        var output = dir.resolve("audit.log");
        try {
            var publisher = FileAuditEventPublisher.of(output, 1, 2);
            publisher.publish(event("select 1"));
            publisher.publish(event("select 2"));
            publisher.publish(event("select 3"));

            assertTrue(Files.exists(output));
            assertTrue(Files.exists(dir.resolve("audit.log.1")));
            assertTrue(Files.exists(dir.resolve("audit.log.2")));
        } finally {
            Files.walk(dir)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                    }
                });
        }
    }

    @Test
    void rotates_without_history_when_max_history_is_zero() throws Exception {
        var dir = Files.createTempDirectory("sqm-audit-rotate-zero");
        var output = dir.resolve("audit.log");
        try {
            var publisher = FileAuditEventPublisher.of(output, 1, 0);
            publisher.publish(event("select 1"));
            publisher.publish(event("select 2"));

            assertTrue(Files.exists(output));
            assertFalse(Files.exists(dir.resolve("audit.log.1")));
        } finally {
            Files.walk(dir)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                    }
                });
        }
    }

    @Test
    void rejects_negative_max_history() throws Exception {
        var output = Files.createTempFile("sqm-audit", ".log");
        try {
            assertThrows(IllegalArgumentException.class, () -> FileAuditEventPublisher.of(output, 10, -1));
        } finally {
            Files.deleteIfExists(output);
        }
    }

    private static AuditEvent event(String sql) {
        return new AuditEvent(
            sql,
            sql,
            java.util.List.of(ReasonCode.NONE),
            null,
            DecisionResult.allow(),
            ExecutionContext.of("postgresql", "alice", "tenant-a", ExecutionMode.ANALYZE, ParameterizationMode.OFF),
            123L
        );
    }
}
