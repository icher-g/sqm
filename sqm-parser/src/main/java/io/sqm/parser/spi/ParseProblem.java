package io.sqm.parser.spi;

/**
 * Represents a parsing problem.
 *
 * @param message an error message.
 * @param pos     a source character offset where the error happened.
 * @param line    a one-based source line where the error happened, or {@code null} when unavailable
 * @param column  a one-based source column where the error happened, or {@code null} when unavailable
 */
public record ParseProblem(String message, int pos, Integer line, Integer column) {
    /**
     * Creates a parsing problem with line and column resolved from the current parse context when available.
     *
     * @param message an error message
     * @param pos a source character offset where the error happened
     */
    public ParseProblem(String message, int pos) {
        this(message, pos, ParseLocations.locate(pos).line(), ParseLocations.locate(pos).column());
    }
}
