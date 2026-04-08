package io.sqm.playground.api;

import java.util.List;

/**
 * Response payload for SQL validation in the playground.
 *
 * @param requestId correlation identifier
 * @param success operation success flag
 * @param durationMs operation duration in milliseconds
 * @param valid whether the SQL is valid for the requested dialect
 * @param diagnostics structured diagnostics
 */
public record ValidateResponseDto(
    String requestId,
    boolean success,
    long durationMs,
    boolean valid,
    List<PlaygroundDiagnosticDto> diagnostics
) implements PlaygroundResponseDto {
}
