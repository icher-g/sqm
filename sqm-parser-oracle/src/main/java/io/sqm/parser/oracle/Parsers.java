package io.sqm.parser.oracle;

import io.sqm.parser.spi.ParsersRepository;

/**
 * Entry point for Oracle parser registrations.
 */
public final class Parsers {

    private static final ParsersRepository defaultRepository = registerOracleOverrides(io.sqm.parser.ansi.Parsers.ansiCopy());

    private Parsers() {
    }

    /**
     * Returns the shared Oracle parsers repository.
     *
     * @return Oracle parsers repository.
     */
    public static ParsersRepository oracle() {
        return defaultRepository;
    }

    /**
     * Returns a new Oracle parsers repository instance with default registrations.
     *
     * @return isolated Oracle parsers repository.
     */
    public static ParsersRepository oracleCopy() {
        return registerOracleOverrides(io.sqm.parser.ansi.Parsers.ansiCopy());
    }

    private static ParsersRepository registerOracleOverrides(ParsersRepository repository) {
        return repository
            .register(new MergeStatementParser())
            .register(new MergeClauseParser())
            .register(new MergeUpdateActionParser())
            .register(new MergeInsertActionParser())
            .register(new LimitOffsetParser());
    }
}
