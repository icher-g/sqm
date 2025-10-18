package io.cherlabs.sqm.parser;

import io.cherlabs.sqm.parser.spi.Lookups;
import io.cherlabs.sqm.parser.spi.ParsersRepository;

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
