package io.sqm.parser.spi;

import io.sqm.core.dialect.DialectCapabilities;

public interface Specs {
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
     * Returns the identifier quoting rules supported by this SQL dialect.
     * <p>
     * The returned {@link IdentifierQuoting} defines how quoted identifiers
     * are recognized and parsed by the lexer, including the opening and
     * closing delimiter characters.
     *
     * @return the dialect-specific identifier quoting configuration.
     */
    IdentifierQuoting identifierQuoting();

    /**
     * Returns dialect capabilities for feature gating.
     *
     * @return dialect capabilities
     */
    DialectCapabilities capabilities();

    /**
     * Returns an operator policy per dialect.
     *
     * @return operator policy.
     */
    OperatorPolicy operatorPolicy();
}
