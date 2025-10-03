package io.cherlabs.sqlmodel.parser;

import io.cherlabs.sqlmodel.core.Entity;
import io.cherlabs.sqlmodel.core.repos.Handler;
import io.cherlabs.sqlmodel.parser.core.Cursor;
import io.cherlabs.sqlmodel.parser.core.Lexer;
import io.cherlabs.sqlmodel.parser.core.ParserException;

import java.util.Objects;

/**
 * A base interface for all spec parsers.
 *
 * @param <T> the type of the entity.
 */
public interface SpecParser<T extends Entity> extends Handler<T> {
    /**
     * A default implementation of the parse method that accepts a spec as a string.
     * The method converts the spec into a {@link Cursor} and calls {@link SpecParser#parse(Cursor)} method.
     *
     * @param spec a spec string
     * @return a parsing result.
     */
    default ParseResult<T> parse(String spec) {
        Objects.requireNonNull(spec, "spec cannot be null.");

        if (spec.isBlank()) {
            return ParseResult.error("The spec cannot be blank.");
        }

        try {
            var ts = Lexer.lexAll(spec);
            return parse(new Cursor(ts));

        } catch (ParserException ex) {
            return ParseResult.error(ex.getMessage());
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
