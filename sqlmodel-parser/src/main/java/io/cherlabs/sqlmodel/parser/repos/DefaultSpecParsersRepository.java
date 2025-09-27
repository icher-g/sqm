package io.cherlabs.sqlmodel.parser.repos;

import io.cherlabs.sqlmodel.core.Entity;
import io.cherlabs.sqlmodel.core.repos.Handler;
import io.cherlabs.sqlmodel.parser.SpecParser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple, thread-safe registry of SpecParsers by (model type).
 */
public class DefaultSpecParsersRepository implements SpecParsersRepository {

    private final Map<Class<?>, Handler<?>> parsers = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> SpecParser<T> get(Class<T> type) {
        var p = parsers.get(type);
        if (p != null) {
            return (SpecParser<T>) p;
        }
        return null;
    }

    @Override
    public <T extends Entity> SpecParsersRepository register(Class<T> type, SpecParser<?> handler) {
        parsers.put(type, handler);
        return this;
    }
}
