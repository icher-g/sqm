package io.cherlabs.sqm.parser;

/**
 * Represents a parsing problem.
 *
 * @param message an error message.
 * @param pos     a position where the error happened.
 */
public record ParseProblem(String message, int pos) {
}
