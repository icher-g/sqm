package io.sqm.parser.spi;

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
}
