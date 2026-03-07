package io.sqm.parser.mysql;

import io.sqm.parser.spi.ParsersRepository;

/**
 * Registry entry point for MySQL parser implementations.
 */
public final class Parsers {

    private static final ParsersRepository defaultRepository = registerDefaults(io.sqm.parser.ansi.Parsers.ansiCopy());

    private Parsers() {
    }

    /**
     * Returns MySQL parser repository with default parser registrations.
     *
     * @return MySQL parser repository.
     */
    public static ParsersRepository mysql() {
        return defaultRepository;
    }

    private static ParsersRepository registerDefaults(ParsersRepository repository) {
        return repository
            .register(new MySqlLimitOffsetParser())
            .register(new MySqlGroupByParser())
            .register(new MySqlRegexPredicateParser())
            .register(new MySqlTableParser())
            .register(new MySqlSelectQueryParser());
    }
}
