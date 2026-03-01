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
    private final long maxBytes;
    private final int maxHistory;
    private final Object writeLock = new Object();

    private FileAuditEventPublisher(Path outputPath, long maxBytes, int maxHistory) {
        this.outputPath = outputPath;
        this.maxBytes = maxBytes;
        this.maxHistory = maxHistory;
    }

    /**
     * Creates a file-backed publisher.
     *
     * @param outputPath output file path
     * @return publisher instance
     */
    public static FileAuditEventPublisher of(Path outputPath) {
        Objects.requireNonNull(outputPath, "outputPath must not be null");
        return new FileAuditEventPublisher(outputPath, 0, 0);
    }

    /**
     * Creates a file-backed publisher with rotation/retention settings.
     *
     * @param outputPath output file path
     * @param maxBytes maximum file size in bytes before rotation; {@code <= 0} disables rotation
     * @param maxHistory number of rotated files retained; {@code 0} keeps no history files
     * @return publisher instance
     */
    public static FileAuditEventPublisher of(Path outputPath, long maxBytes, int maxHistory) {
        Objects.requireNonNull(outputPath, "outputPath must not be null");
        if (maxHistory < 0) {
            throw new IllegalArgumentException("maxHistory must be >= 0");
        }
        return new FileAuditEventPublisher(outputPath, maxBytes, maxHistory);
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
                rotateIfNeeded(line.getBytes(StandardCharsets.UTF_8).length);
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

    private void rotateIfNeeded(int additionalBytes) throws IOException {
        if (maxBytes <= 0 || !Files.exists(outputPath)) {
            return;
        }
        long currentSize = Files.size(outputPath);
        if (currentSize + additionalBytes <= maxBytes) {
            return;
        }

        if (maxHistory <= 0) {
            Files.deleteIfExists(outputPath);
            return;
        }

        for (int index = maxHistory; index >= 1; index--) {
            var target = rotatedPath(index);
            if (index == 1) {
                if (Files.exists(outputPath)) {
                    Files.move(outputPath, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                var previous = rotatedPath(index - 1);
                if (Files.exists(previous)) {
                    Files.move(previous, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private Path rotatedPath(int index) {
        var fileName = outputPath.getFileName();
        var baseName = fileName == null ? "audit.log" : fileName.toString();
        return outputPath.resolveSibling(baseName + "." + index);
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
