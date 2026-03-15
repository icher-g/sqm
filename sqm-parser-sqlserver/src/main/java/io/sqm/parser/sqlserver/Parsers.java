package io.sqm.parser.sqlserver;

import io.sqm.parser.spi.ParsersRepository;

/**
 * Registry entry point for SQL Server parser implementations.
 */
public final class Parsers {

    private static final ParsersRepository DEFAULT_REPOSITORY = registerDefaults(io.sqm.parser.ansi.Parsers.ansiCopy());

    private Parsers() {
    }

    /**
     * Returns SQL Server parser repository with default parser registrations.
     *
     * @return SQL Server parser repository.
     */
    public static ParsersRepository sqlServer() {
        return DEFAULT_REPOSITORY;
    }

    private static ParsersRepository registerDefaults(ParsersRepository repository) {
        return repository
            .register(new SqlServerFunctionExprParser())
            .register(new SqlServerLimitOffsetParser())
            .register(new SqlServerSelectQueryParser());
    }
}
