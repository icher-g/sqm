package io.sqm.parser.core;

import io.sqm.parser.spi.ParseLocations;

/**
 * Represents a parsing exception.
 */
public class ParserException extends RuntimeException {

    /** Parsing error message. */
    private final String message;

    /** Source character offset where error occurred. */
    private final int pos;
    /** One-based source line where error occurred. */
    private final Integer line;
    /** One-based source column where error occurred. */
    private final Integer column;

    /**
     * Creates parser exception with error message and source character offset.
     *
     * @param message error message
     * @param pos source character offset where error occurred
     */
    public ParserException(String message, int pos) {
        this(message, pos, ParseLocations.locate(pos).line(), ParseLocations.locate(pos).column());
    }

    /**
     * Creates parser exception with error message, source character offset, and source line and column.
     *
     * @param message error message
     * @param pos source character offset where error occurred
     * @param line one-based source line where error occurred
     * @param column one-based source column where error occurred
     */
    public ParserException(String message, int pos, Integer line, Integer column) {
        this.message = message;
        this.pos = pos;
        this.line = line;
        this.column = column;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Returns the source character offset where parsing failed.
     *
     * @return source character offset
     */
    public int getPos() {
        return pos;
    }

    /**
     * Returns the one-based source line where parsing failed.
     *
     * @return source line, or {@code null} when unavailable
     */
    public Integer getLine() {
        return line;
    }

    /**
     * Returns the one-based source column where parsing failed.
     *
     * @return source column, or {@code null} when unavailable
     */
    public Integer getColumn() {
        return column;
    }

    @Override
    public String toString() {
        return message + " at " + pos;
    }
}
