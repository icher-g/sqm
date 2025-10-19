package io.sqm.parser.ansi;

import io.sqm.parser.spi.Specs;
import io.sqm.parser.spi.Lookups;
import io.sqm.parser.spi.ParsersRepository;

public class AnsiSpecs implements Specs {
    /**
     * Gets a parser's repository.
     *
     * @return a parser's repository.
     */
    @Override
    public ParsersRepository parsers() {
        return Parsers.ansi();
    }

    /**
     * Gets {@link Lookups} implementation.
     *
     * @return lookups implementation.
     */
    @Override
    public Lookups lookups() {
        return new AnsiLookups();
    }
}
