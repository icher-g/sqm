package io.cherlabs.sqm.parser;

import io.cherlabs.sqm.core.Entity;
import io.cherlabs.sqm.core.repos.Handler;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.core.Lexer;
import io.cherlabs.sqm.parser.core.ParserException;

import java.util.Objects;

/**
 * A base interface for all spec parsers.
 *
 * @param <T> the type of the entity.
 */
public interface Parser<T extends Entity> extends Handler<T> {
    /**
     * A default implementation of the parse method that accepts a spec as a string.
     * The method converts the spec into a {@link Cursor} and calls {@link Parser#parse(Cursor)} method.
     *
     * @param spec a spec string
     * @return a parsing result.
     */
    default ParseResult<T> parse(String spec) {
        Objects.requireNonNull(spec, "spec cannot be null.");

        if (spec.isBlank()) {
            return ParseResult.error("The spec cannot be blank.", -1);
        }

        try {
            var ts = Lexer.lexAll(spec);
            return parse(new Cursor(ts));
        } catch (ParserException ex) {
            return ParseResult.error(ex);
        }
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @return a parsing result.
     */
    ParseResult<T> parse(Cursor cur);
}
