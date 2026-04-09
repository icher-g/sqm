package io.sqm.playground.api;

import java.util.List;

/**
 * Response payload for playground liveness checks.
 *
 * @param requestId correlation identifier
 * @param success operation success flag
 * @param durationMs operation duration in milliseconds
 * @param status host status text
 * @param diagnostics structured diagnostics
 */
public record HealthResponseDto(
    String requestId,
    boolean success,
    long durationMs,
    String status,
    List<PlaygroundDiagnosticDto> diagnostics
) implements PlaygroundResponseDto {
}
