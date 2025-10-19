package io.sqm.parser.spi;

import io.sqm.core.Entity;
import io.sqm.parser.DefaultParseContext;
import io.sqm.parser.core.Cursor;

/**
 * A parsing context passed to the {@link Parser}.
 */
public interface ParseContext {

    /**
     * Creates a new instance of the {@link ParseContext} with the provided {@link Specs} implementation.
     *
     * @param specs dialect aware specs.
     * @return a new instance of the {@link ParseContext}.
     */
    static ParseContext of(Specs specs) {
        return new DefaultParseContext(specs);
    }

    /**
     * Gets a parser's repository.
     *
     * @return a parser's repository.
     */
    ParsersRepository parsers();

    /**
     * Gets {@link Lookups} implementation.
     *
     * @return lookups implementation.
     */
    Lookups lookups();

    /**
     * Gets a number of calls made in the current stack. This is used to indicate whether the parser is being called from another parser, meaning that the current cursor might have extra information.
     * Each time the {@link this#parse(Class, Cursor)} is called the {@link this#increaseCallstack()} method is called and each time
     * the execution of the {@link this#parse(Class, Cursor)} is completed the {@link this#decreaseCallstack()} is executed.
     *
     * @return a current number of calls in a callstack.
     */
    int callstack();

    /**
     * Increases a callstack number.
     */
    void increaseCallstack();

    /**
     * Decreases a callstack number.
     */
    void decreaseCallstack();

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @return a parsing result.
     */
    default <T extends Entity> ParseResult<T> parse(Class<T> type, Cursor cur) {
        increaseCallstack();
        try {
            var parser = parsers().require(type);
            return parser.parse(cur, this);
        } finally {
            decreaseCallstack();
        }
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param spec a spec to be parsed.
     * @return a parsing result.
     */
    default <T extends Entity> ParseResult<T> parse(Class<T> type, String spec) {
        var parser = parsers().require(type);
        return parser.parse(spec, this);
    }
}
