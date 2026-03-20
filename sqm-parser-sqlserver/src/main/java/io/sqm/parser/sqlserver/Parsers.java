package io.sqm.parser.sqlserver;

import io.sqm.parser.PostfixExprParser;
import io.sqm.parser.spi.ParsersRepository;
import io.sqm.parser.ansi.PowerArithmeticExprParser;

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
        var atomicExprParser = new AtomicExprParser();
        var postfixExprParser = new PostfixExprParser(atomicExprParser);
        return repository
            .register(new InsertStatementParser())
            .register(new UpdateStatementParser())
            .register(new DeleteStatementParser())
            .register(new MergeStatementParser())
            .register(new TableParser())
            .register(new MergeClauseParser())
            .register(new MergeUpdateActionParser())
            .register(new MergeDeleteActionParser())
            .register(new MergeInsertActionParser())
            .register(new OutputColumnExprParser())
            .register(new OutputStarResultItemParser())
            .register(new FunctionExprParser())
            .register(new LimitOffsetParser())
            .register(new PowerArithmeticExprParser(postfixExprParser))
            .register(new SelectQueryParser())
            .register(new ResultClauseParser())
            .register(new ResultIntoParser());
    }
}
