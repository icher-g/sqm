package io.sqm.parser;

import io.sqm.core.Node;
import io.sqm.core.repos.Handler;
import io.sqm.parser.spi.Parser;
import io.sqm.parser.spi.ParsersRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple, thread-safe registry of SpecParsers by (model type).
 */
public class DefaultParsersRepository implements ParsersRepository {

    private final Map<Class<?>, Handler<?>> parsers = new ConcurrentHashMap<>();

    /**
     * Creates an empty parser repository.
     */
    public DefaultParsersRepository() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Node> Parser<T> get(Class<T> type) {
        var p = parsers.get(type);
        if (p != null) {
            return (Parser<T>) p;
        }
        return null;
    }

    @Override
    public <T extends Node> ParsersRepository register(Class<T> type, Parser<?> handler) {
        parsers.put(type, handler);
        return this;
    }
}
