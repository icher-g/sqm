package io.sqm.parser;

import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.parser.spi.*;

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
     * Returns the identifier quoting rules supported by this SQL dialect.
     * <p>
     * The returned {@link IdentifierQuoting} defines how quoted identifiers
     * are recognized and parsed by the lexer, including the opening and
     * closing delimiter characters.
     *
     * @return the dialect-specific identifier quoting configuration.
     */
    @Override
    public IdentifierQuoting identifierQuoting() {
        return specs.identifierQuoting();
    }

    /**
     * Returns the dialect capabilities used for feature gating.
     *
     * @return dialect capabilities
     */
    @Override
    public DialectCapabilities capabilities() {
        return specs.capabilities();
    }

    /**
     * Returns an operator policy per dialect.
     *
     * @return operator policy.
     */
    @Override
    public OperatorPolicy operatorPolicy() {
        return specs.operatorPolicy();
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
