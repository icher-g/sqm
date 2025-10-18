package io.cherlabs.sqm.parser;

import io.cherlabs.sqm.parser.spi.Lookups;
import io.cherlabs.sqm.parser.spi.ParseContext;
import io.cherlabs.sqm.parser.spi.ParsersRepository;

/**
 * Default implementation of the {@link ParseContext}.
 */
public class DefaultParseContext implements ParseContext {

    private final Specs specs;
    private int callstack;

    /**
     * Creates an instance of the class with the provided {@link Specs}.
     *
     * @param specs dialect aware specs.
     */
    public DefaultParseContext(Specs specs) {
        this.specs = specs;
    }

    @Override
    public ParsersRepository parsers() {
        return specs.parsers();
    }

    @Override
    public Lookups lookups() {
        return specs.lookups();
    }

    @Override
    public int callstack() {
        return callstack;
    }

    @Override
    public void increaseCallstack() {
        callstack++;
    }

    @Override
    public void decreaseCallstack() {
        callstack = Math.max(0, callstack - 1);
    }
}
