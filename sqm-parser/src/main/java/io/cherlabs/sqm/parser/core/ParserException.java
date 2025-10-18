package io.cherlabs.sqm.parser.core;


/**
 * Represents a parsing exception.
 */
public class ParserException extends RuntimeException {

    private final String message;
    private final int pos;

    public ParserException(String message, int pos) {
        this.message = message;
        this.pos = pos;
    }

    public String getMessage() {
        return message;
    }

    public int getPos() {
        return pos;
    }

    @Override
    public String toString() {
        return message + " at " + pos;
    }
}
