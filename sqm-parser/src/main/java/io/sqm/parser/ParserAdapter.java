package io.sqm.parser;

import io.sqm.core.Node;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.Objects;

/**
 * Utility factory for adapting parser target types without changing parsing behavior.
 */
public final class ParserAdapter {

    private ParserAdapter() {
    }

    /**
     * Wraps a parser and exposes it under a wider target type.
     *
     * @param targetType the wider target type to expose
     * @param delegate the parser to delegate to
     * @param <T> the exposed target type
     * @param <R> the delegate target type
     * @return a parser that preserves the delegate behavior while widening its target type
     */
    public static <T extends Node, R extends T> Parser<T> widen(Class<? extends T> targetType, Parser<R> delegate) {
        Objects.requireNonNull(targetType, "targetType");
        Objects.requireNonNull(delegate, "delegate");
        return new Parser<>() {
            @Override
            public ParseResult<? extends T> parse(Cursor cur, ParseContext ctx) {
                return delegate.parse(cur, ctx);
            }

            @Override
            public Class<? extends T> targetType() {
                return targetType;
            }
        };
    }
}
