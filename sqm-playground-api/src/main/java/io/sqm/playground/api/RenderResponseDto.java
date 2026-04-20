package io.sqm.playground.api;

import java.util.List;

/**
 * Response payload for SQL rendering in the playground.
 *
 * @param requestId correlation identifier
 * @param success operation success flag
 * @param durationMs operation duration in milliseconds
 * @param renderedSql rendered SQL output
 * @param params rendered SQL bind parameters in order
 * @param diagnostics structured diagnostics
 */
public record RenderResponseDto(
    String requestId,
    boolean success,
    long durationMs,
    String renderedSql,
    List<Object> params,
    List<PlaygroundDiagnosticDto> diagnostics
) implements PlaygroundResponseDto {
}
