package io.sqm.parser.ansi;

import io.sqm.parser.spi.IdentifierQuoting;
import io.sqm.parser.spi.Lookups;
import io.sqm.parser.spi.ParsersRepository;
import io.sqm.parser.spi.Specs;

public class AnsiSpecs implements Specs {

    private Lookups lookups;
    private IdentifierQuoting identifierQuoting;

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
        if (lookups == null) {
            lookups = new AnsiLookups();
        }
        return lookups;
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
        if (identifierQuoting == null) {
            identifierQuoting = IdentifierQuoting.of('"');
        }
        return identifierQuoting;
    }
}
