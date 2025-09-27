package io.cherlabs.sqlmodel.parser;

import io.cherlabs.sqlmodel.core.Entity;
import io.cherlabs.sqlmodel.core.repos.Handler;
import io.cherlabs.sqlmodel.parser.core.Cursor;
import io.cherlabs.sqlmodel.parser.core.Lexer;
import io.cherlabs.sqlmodel.parser.core.ParserException;

import java.util.Objects;

public interface SpecParser<T extends Entity> extends Handler<T> {
    Class<T> targetType();

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

    ParseResult<T> parse(Cursor cur);
}
