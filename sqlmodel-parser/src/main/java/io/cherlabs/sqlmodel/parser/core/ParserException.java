package io.cherlabs.sqlmodel.parser.core;


/**
 * Represents a parsing exception.
 */
public class ParserException extends RuntimeException {
    public ParserException(String message, int pos) {
        super(message + " at " + pos);
    }
}
