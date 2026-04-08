package io.sqm.playground.api;

import java.util.List;

/**
 * Response payload for SQL transpilation in the playground.
 *
 * @param requestId correlation identifier
 * @param success operation success flag
 * @param durationMs operation duration in milliseconds
 * @param outcome transpilation outcome classification
 * @param renderedSql transpiled SQL output when available
 * @param diagnostics structured diagnostics
 */
public record TranspileResponseDto(
    String requestId,
    boolean success,
    long durationMs,
    TranspileOutcomeDto outcome,
    String renderedSql,
    List<PlaygroundDiagnosticDto> diagnostics
) implements PlaygroundResponseDto {
}
