package io.sqm.control.audit;

import io.sqm.control.AuditEvent;
import io.sqm.control.AuditEventPublisher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Objects;

/**
 * Durable audit publisher that appends one JSON line per event to a file.
 */
public final class FileAuditEventPublisher implements AuditEventPublisher {

    private final Path outputPath;
    private final Object writeLock = new Object();

    private FileAuditEventPublisher(Path outputPath) {
        this.outputPath = outputPath;
    }

    /**
     * Creates a file-backed publisher.
     *
     * @param outputPath output file path
     * @return publisher instance
     */
    public static FileAuditEventPublisher of(Path outputPath) {
        Objects.requireNonNull(outputPath, "outputPath must not be null");
        return new FileAuditEventPublisher(outputPath);
    }

    /**
     * Appends one event to the configured file.
     *
     * @param event audit event
     */
    @Override
    public void publish(AuditEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        var line = toJsonLine(event) + System.lineSeparator();
        synchronized (writeLock) {
            try {
                var parent = outputPath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(
                    outputPath,
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
                );
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to append audit event to file: " + outputPath, ex);
            }
        }
    }

    private String toJsonLine(AuditEvent event) {
        var decision = event.decision();
        var context = event.context();
        var fingerprint = decision.fingerprint() == null ? "" : decision.fingerprint();
        return "{"
            + "\"timestamp\":\"" + escapeJson(Instant.now().toString()) + "\","
            + "\"decisionKind\":\"" + decision.kind() + "\","
            + "\"reasonCode\":\"" + decision.reasonCode() + "\","
            + "\"durationNanos\":" + event.durationNanos() + ","
            + "\"dialect\":\"" + escapeJson(context.dialect()) + "\","
            + "\"principal\":\"" + escapeJson(nullToEmpty(context.principal())) + "\","
            + "\"tenant\":\"" + escapeJson(nullToEmpty(context.tenant())) + "\","
            + "\"mode\":\"" + context.mode() + "\","
            + "\"fingerprint\":\"" + escapeJson(fingerprint) + "\","
            + "\"sql\":\"" + escapeJson(event.normalizedSql()) + "\""
            + "}";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String escapeJson(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n")
            .replace("\t", "\\t");
    }
}
