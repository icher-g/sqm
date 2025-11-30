package io.sqm.parser;

import io.sqm.parser.spi.Lookups;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParsersRepository;
import io.sqm.parser.spi.Specs;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Default implementation of the {@link ParseContext}.
 */
public class DefaultParseContext implements ParseContext {

    private final Specs specs;
    private final Deque<Class<?>> callstack;

    /**
     * Creates an instance of the class with the provided {@link Specs}.
     *
     * @param specs dialect aware specs.
     */
    public DefaultParseContext(Specs specs) {
        this.specs = specs;
        this.callstack = new ArrayDeque<>();
    }

    @Override
    public ParsersRepository parsers() {
        return specs.parsers();
    }

    @Override
    public Lookups lookups() {
        return specs.lookups();
    }

    /**
     * Gets a current callstack.
     *
     * @return a current callstack.
     */
    @Override
    public Deque<Class<?>> callstack() {
        return callstack;
    }
}
