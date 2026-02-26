package io.sqm.parser.postgresql;

import io.sqm.parser.spi.ParsersRepository;

/**
 * Registry entry point for PostgreSQL parser implementations.
 */
public class Parsers {

    private static final ParsersRepository defaultRepository = registerDefaults(io.sqm.parser.ansi.Parsers.ansi());

    private Parsers() {
    }

    /**
     * Returns PostgreSQL parser repository with default parser registrations.
     *
     * @return PostgreSQL parser repository.
     */
    public static ParsersRepository postgres() {
        return defaultRepository;
    }

    private static ParsersRepository registerDefaults(ParsersRepository r) {
        return r
            .register(new BinaryOperatorExprParser())
            .register(new CastExprParser());
    }
}
