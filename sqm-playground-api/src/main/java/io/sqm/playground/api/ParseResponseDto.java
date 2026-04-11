package io.sqm.playground.api;

import java.util.List;

/**
 * Response payload for SQL parsing in the playground.
 *
 * @param requestId correlation identifier
 * @param success operation success flag
 * @param durationMs operation duration in milliseconds
 * @param statementKind high-level statement family
 * @param sqmJson serialized SQM JSON
 * @param sqmDsl generated SQM DSL calls
 * @param ast AST tree representation
 * @param summary parse summary metadata
 * @param diagnostics structured diagnostics
 */
public record ParseResponseDto(
    String requestId,
    boolean success,
    long durationMs,
    String statementKind,
    String sqmJson,
    String sqmDsl,
    AstNodeDto ast,
    ParseResponseSummaryDto summary,
    List<PlaygroundDiagnosticDto> diagnostics
) implements PlaygroundResponseDto {
}
