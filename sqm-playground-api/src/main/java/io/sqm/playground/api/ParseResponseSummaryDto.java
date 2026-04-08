package io.sqm.playground.api;

/**
 * Summary metadata for a successful parse response.
 *
 * @param rootNodeType root AST node type
 * @param rootInterface root SQM interface name
 */
public record ParseResponseSummaryDto(
    String rootNodeType,
    String rootInterface
) {
}
