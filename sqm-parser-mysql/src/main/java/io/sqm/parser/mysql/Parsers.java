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
            .register(new ConcatExprParser())
            .register(new IntervalLiteralExprParser())
            .register(new InsertStatementParser())
            .register(new UpdateStatementParser())
            .register(new DeleteStatementParser())
            .register(new LimitOffsetParser())
            .register(new GroupByParser())
            .register(new RegexPredicateParser())
            .register(new TableParser())
            .register(new SelectQueryParser());
    }
}
