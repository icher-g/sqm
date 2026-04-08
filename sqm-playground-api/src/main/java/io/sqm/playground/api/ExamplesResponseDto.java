package io.sqm.playground.api;

import java.util.List;

/**
 * Response payload for built-in playground examples.
 *
 * @param requestId correlation identifier
 * @param success operation success flag
 * @param durationMs operation duration in milliseconds
 * @param examples built-in examples
 * @param diagnostics structured diagnostics
 */
public record ExamplesResponseDto(
    String requestId,
    boolean success,
    long durationMs,
    List<ExampleDto> examples,
    List<PlaygroundDiagnosticDto> diagnostics
) implements PlaygroundResponseDto {
}
