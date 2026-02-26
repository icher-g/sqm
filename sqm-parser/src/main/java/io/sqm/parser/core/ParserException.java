package io.sqm.parser.core;


/**
 * Represents a parsing exception.
 */
public class ParserException extends RuntimeException {

    /** Parsing error message. */
    private final String message;

    /** Token position where error occurred. */
    private final int pos;

    /**
     * Creates parser exception with error message and token position.
     *
     * @param message error message
     * @param pos token position where error occurred
     */
    public ParserException(String message, int pos) {
        this.message = message;
        this.pos = pos;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Returns source token position where parsing failed.
     *
     * @return token position
     */
    public int getPos() {
        return pos;
    }

    @Override
    public String toString() {
        return message + " at " + pos;
    }
}
